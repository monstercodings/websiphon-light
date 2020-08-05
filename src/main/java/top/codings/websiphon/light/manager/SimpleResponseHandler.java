package top.codings.websiphon.light.manager;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.processor.IProcessor;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 分化处理响应的处理器
 */
@Slf4j
public abstract class SimpleResponseHandler extends ChainResponseHandler {
    protected List<Function<BuiltinRequest, BuiltinRequest>> successes = new CopyOnWriteArrayList<>();
    protected List<Function<BuiltinRequest, BuiltinRequest>> errors = new CopyOnWriteArrayList<>();

    public SimpleResponseHandler() {
        IProcessor processor = processorChain();
        successes.add(builtinRequest -> {
            BuiltinRequest.RequestResult result = builtinRequest.getRequestResult();
            Object data = result.get();
            processor.process(data, builtinRequest, crawler);
            return builtinRequest;
        });
        errors.add(builtinRequest -> {
            BuiltinRequest.RequestResult result = builtinRequest.getRequestResult();
            Throwable throwable = result.cause();
            handleError(builtinRequest, throwable);
//            log.error("发生异常 -> {}", throwable.getClass());
            return builtinRequest;
        });
    }

    @Override
    protected void handle(BuiltinRequest request) throws Exception {
        BuiltinRequest.RequestResult result = request.getRequestResult();
        if (result.isSucceed()) {
            handleSucceed(request);
        } else {
            handleError(request);
        }
    }

    private void handleSucceed(BuiltinRequest request) {
        BuiltinRequest builtinRequest = request;
        try {
            for (Function<BuiltinRequest, BuiltinRequest> success : successes) {
                if ((builtinRequest = success.apply(builtinRequest)) == null) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("响应处理器发生异常", e);
        }
    }

    private void handleError(BuiltinRequest request) {
        try {
            BuiltinRequest builtinRequest = request;
            for (Function<BuiltinRequest, BuiltinRequest> error : errors) {
                if ((builtinRequest = error.apply(builtinRequest)) == null) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("处理异常失败", e);
        }
    }

    /**
     * 返回处理器链
     * @return
     */
    protected abstract IProcessor processorChain();

    /**
     * 处理异常状态
     * @param request
     * @param throwable
     */
    protected abstract void handleError(BuiltinRequest request, Throwable throwable);
}
