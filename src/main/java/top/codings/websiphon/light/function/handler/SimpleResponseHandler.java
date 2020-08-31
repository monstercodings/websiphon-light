package top.codings.websiphon.light.function.handler;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.processor.support.AbstractProcessor;
import top.codings.websiphon.light.function.processor.IProcessor;
import top.codings.websiphon.light.function.processor.ProcessCloseAware;
import top.codings.websiphon.light.function.processor.ProcessInitAware;
import top.codings.websiphon.light.requester.IRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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
    public CompletableFuture<IResponseHandler> startup(ICrawler crawler) {
        CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
            processor = processorChain();
            successes.add(request -> {
                IRequest.RequestResult result = request.getRequestResult();
                Object data = result.get();
                processor.process(data, request, crawler);
                return request;
            });
            errors.add(request -> {
                IRequest.RequestResult result = request.getRequestResult();
                Throwable throwable = result.cause();
                handleError(request, throwable, crawler);
//            log.error("发生异常 -> {}", throwable.getClass());
                return request;
            });
            if (processor instanceof AbstractProcessor) {
                try {
                    ((AbstractProcessor) processor).initByHandler(crawler);
                } catch (Exception e) {
                    throw new FrameworkException("ProcessInitAware类型处理器初始化异常", e);
                }
            } else if (processor instanceof ProcessInitAware) {
                try {
                    ((ProcessInitAware) processor).init(crawler);
                } catch (Exception e) {
                    throw new FrameworkException("ProcessInitAware类型处理器初始化异常", e);
                }
            }
            return this;
        });
        return completableFuture.thenCombineAsync(super.startup(crawler), (o, o2) -> o);
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
    public CompletableFuture<IResponseHandler> shutdown(boolean force) {
        return super.shutdown(force).thenCombineAsync(CompletableFuture.supplyAsync(() -> {
            if (processor instanceof AbstractProcessor) {
                try {
                    ((AbstractProcessor) processor).closeByHandler();
                } catch (Exception e) {
                    log.error("ProcessCloseAware类型处理器关闭时发生异常");
                }
            } else if (processor instanceof ProcessCloseAware) {
                try {
                    ((ProcessCloseAware) processor).close();
                } catch (Exception e) {
                    log.error("ProcessCloseAware类型处理器关闭时发生异常");
                }
            }
            return this;
        }), (iResponseHandler, simpleResponseHandler) -> simpleResponseHandler);
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
