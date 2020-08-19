package top.codings.websiphon.light.requester.support;

import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;

import java.util.concurrent.CompletableFuture;

public abstract class CombineRequester<T extends IRequest> implements IRequester<T> {
    protected volatile boolean shutdown;
    protected CombineRequester<T> requester;
    private Class<T> requestClass;
    private IRequester.NetworkErrorStrategy strategy = IRequester.NetworkErrorStrategy.DROP;

    protected CombineRequester(CombineRequester requester) {
        this.requester = requester;
        /*Class currentClass = this.getClass();
        Type genericSuperType = currentClass.getGenericSuperclass();
        if (!(genericSuperType instanceof ParameterizedType)) {
            throw new FrameworkException("泛型读取异常");
        }
        Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();
        Type actualTypeParam = actualTypeParams[0];
        if (actualTypeParam instanceof ParameterizedType) {
            actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
        }
        if (actualTypeParam instanceof Class) {
            requestClass = (Class<T>) actualTypeParam;
        }
        System.out.println(this.getClass().getName() + " -> " + requestClass.getName());*/
    }

    @Override
    public CompletableFuture<IRequester> init() {
        if (null != requester) {
            return requester.init();
        }
        throw new FrameworkException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public CompletableFuture<T> execute(T request) {
        if (null != requester) {
            return requester.execute(request);
        }
        throw new FrameworkException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public T create(String url) {
        if (null != requester) {
            return requester.create(url);
        }
        throw new FrameworkException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public T create(String url, Object userData) {
        if (null != requester) {
            return requester.create(url, userData);
        }
        throw new FrameworkException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public CompletableFuture<IRequester> shutdown(boolean force) {
        if (null != requester) {
            return requester.shutdown(force);
        }
        throw new FrameworkException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public boolean isBusy() {
        if (null != requester) {
            return requester.isBusy();
        }
        return false;
    }

    @Override
    public void setResponseHandler(IResponseHandler responseHandler) {
        if (null != requester) {
            requester.setResponseHandler(responseHandler);
            return;
        }
        throw new FrameworkException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public IResponseHandler getResponseHandler() {
        if (null != requester) {
            return requester.getResponseHandler();
        }
        throw new FrameworkException("非代理请求器必须实现自身执行逻辑");
    }

    public final NetworkErrorStrategy getStrategy() {
        return strategy;
    }

    public final void setStrategy(NetworkErrorStrategy strategy) {
        this.strategy = strategy;
    }
}
