package top.codings.websiphon.light.function.handler;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.error.StopHandlErrorException;
import top.codings.websiphon.light.function.ComponentCloseAware;
import top.codings.websiphon.light.function.ComponentErrorAware;
import top.codings.websiphon.light.function.ComponentInitAware;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.function.processor.IProcessor;
import top.codings.websiphon.light.requester.IRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * 分化处理响应的处理器
 */
@Slf4j
public abstract class SimpleResponseHandler extends ChainResponseHandler {
    protected List<Function<IRequest, IRequest>> successes = new CopyOnWriteArrayList<>();
    protected List<Function<IRequest, IRequest>> errors = new CopyOnWriteArrayList<>();
    private IProcessor processor;

    @Override
    public void init(ICrawler crawler) throws Exception {
        super.init(crawler);
        if (!tryInit()) {
            return;
        }
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
            }
        });
        errors.add(request -> {
            IRequest.RequestResult result = request.getRequestResult();
            Throwable throwable = result.cause();
            handleError(request, throwable, crawler);
            return request;
        });
        if (processor instanceof AbstractProcessor) {
            try {
                ((AbstractProcessor) processor).initByHandler(crawler);
            } catch (Exception e) {
                throw new FrameworkException("ProcessInitAware类型处理器初始化异常", e);
            }
        } else if (processor instanceof ComponentInitAware) {
            try {
                ((ComponentInitAware) processor).init(crawler);
            } catch (Exception e) {
                throw new FrameworkException("ProcessInitAware类型处理器初始化异常", e);
            }
        }
    }

    private transient AtomicBoolean firstInit = new AtomicBoolean(true);

    private boolean tryInit() {
        return firstInit.compareAndExchange(true, false);
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
        try {
            for (Function<IRequest, IRequest> success : successes) {
                if ((req = success.apply(req)) == null) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("响应处理器发生异常", e);
        }
    }

    private void handleError(IRequest request) {
        try {
            IRequest req = request;
            for (Function<IRequest, IRequest> error : errors) {
                if ((req = error.apply(req)) == null) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("处理异常失败", e);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (processor instanceof AbstractProcessor) {
            try {
                ((AbstractProcessor) processor).closeByHandler();
            } catch (Exception e) {
                log.error("ProcessCloseAware类型处理器关闭时发生异常");
            }
        } else if (processor instanceof ComponentCloseAware) {
            try {
                ((ComponentCloseAware) processor).close();
            } catch (Exception e) {
                log.error("ProcessCloseAware类型处理器关闭时发生异常");
            }
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
