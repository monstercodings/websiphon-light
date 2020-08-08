package top.codings.websiphon.light.manager;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.processor.IProcessor;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 分化处理响应的处理器
 */
@Slf4j
public abstract class SimpleResponseHandler extends ChainResponseHandler {
    protected List<Function<IRequest, IRequest>> successes = new CopyOnWriteArrayList<>();
    protected List<Function<IRequest, IRequest>> errors = new CopyOnWriteArrayList<>();

    @Override
    public void startup(ICrawler crawler) {
        IProcessor processor = processorChain();
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
        super.startup(crawler);
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
