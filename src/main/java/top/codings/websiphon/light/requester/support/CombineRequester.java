package top.codings.websiphon.light.requester.support;

import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;

import java.util.concurrent.CompletableFuture;

public abstract class CombineRequester<T extends IRequest> implements IRequester<T> {
    protected CombineRequester<T> requester;

    protected CombineRequester(CombineRequester requester) {
        this.requester = requester;
    }

    @Override
    public void init() {
        if (null != requester) {
            requester.init();
            return;
        }
        throw new RuntimeException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public CompletableFuture<T> executeAsync(T request) {
        if (null != requester) {
            return requester.executeAsync(request);
        }
        throw new RuntimeException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public void shutdown(boolean force) {
        if (null != requester) {
            requester.shutdown(force);
            return;
        }
        throw new RuntimeException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public boolean isBusy() {
        if (null != requester) {
            return requester.isBusy();
        }
        throw new RuntimeException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public IResponseHandler getResponseHandler() {
        if (null != requester) {
            return requester.getResponseHandler();
        }
        throw new RuntimeException("非代理请求器必须实现自身执行逻辑");
    }
}
