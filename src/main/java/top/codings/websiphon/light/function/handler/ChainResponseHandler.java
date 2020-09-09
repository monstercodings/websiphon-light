package top.codings.websiphon.light.function.handler;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.error.StopHandlErrorException;
import top.codings.websiphon.light.function.ComponentCloseAware;
import top.codings.websiphon.light.function.ComponentErrorAware;
import top.codings.websiphon.light.function.ComponentInitAware;
import top.codings.websiphon.light.function.processor.IProcessor;
import top.codings.websiphon.light.requester.IRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 具备链式调用能力的分化处理响应管理器
 */
@Slf4j
public abstract class ChainResponseHandler extends AsyncResponseHandler {
    protected List<Function<IRequest, IRequest>> successes = new CopyOnWriteArrayList<>();
    protected List<Function<IRequest, IRequest>> errors = new CopyOnWriteArrayList<>();
    private IProcessor processor;

    @Override
    protected void init(ICrawler crawler, int index) throws Exception {
        super.init(crawler, index);
        // 利用锁机制确保处理器链能够串行初始化
        // 避免并发初始化造成的初始化索引无法对应关闭索引的问题
        synchronized (this) {
            // 确保响应管理器只初始化一次
            if (index == 0) {
                processor = processorChain();
                successes.add(request -> {
                    IRequest.RequestResult result = request.getRequestResult();
                    Object data = result.get();
                    processor.process(data, request, crawler);
                    return request;
                });
                errors.add(request -> {
                    try {
                        if (processor instanceof ComponentErrorAware) {
                            ((ComponentErrorAware) processor).doOnError(
                                    request.getRequestResult().cause(), request, crawler);
                        }
                        return request;
                    } catch (StopHandlErrorException e) {
                        return null;
                    } catch (Exception e) {
                        throw new FrameworkException(e);
                    }
                });
                errors.add(request -> {
                    IRequest.RequestResult result = request.getRequestResult();
                    Throwable throwable = result.cause();
                    handleError(request, throwable, crawler);
                    return request;
                });
            }
            if (processor instanceof ComponentInitAware) {
                ((ComponentInitAware) processor).init(crawler);
            }
        }
    }

    @Override
    protected void handle(IRequest request, ICrawler crawler) throws Exception {
        IRequest.RequestResult result = request.getRequestResult();
        if (result.isSucceed()) {
            handleSucceed(request);
        } else {
            handleError(request);
        }
    }

    private void handleSucceed(IRequest request) {
        IRequest req = request;
        for (Function<IRequest, IRequest> success : successes) {
            if ((req = success.apply(req)) == null) {
                break;
            }
        }
    }

    private void handleError(IRequest request) throws Exception {
        try {
            IRequest req = request;
            for (Function<IRequest, IRequest> error : errors) {
                if ((req = error.apply(req)) == null) {
                    break;
                }
            }
        } catch (FrameworkException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    @Override
    protected void close(int index) throws Exception {
        super.close(index);
        if (processor instanceof ComponentCloseAware) {
            ((ComponentCloseAware) processor).close();
        }
        if (index == 0) {
            successes.clear();
            errors.clear();
            processor = null;
        }
    }

    /**
     * 返回处理器链
     *
     * @return
     */
    protected abstract IProcessor processorChain();

    /**
     * 处理异常状态
     *
     * @param request
     * @param throwable
     */
    protected abstract void handleError(IRequest request, Throwable throwable, ICrawler crawler);
}
