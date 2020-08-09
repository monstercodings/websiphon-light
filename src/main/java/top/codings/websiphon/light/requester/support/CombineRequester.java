package top.codings.websiphon.light.requester.support;

import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

public abstract class CombineRequester<T extends IRequest> implements IRequester<T> {
    protected CombineRequester<T> requester;
    private Class<T> requestClass;

    protected CombineRequester(CombineRequester requester) {
        this.requester = requester;
        Class currentClass = this.getClass();
        Type genericSuperType = currentClass.getGenericSuperclass();
        if (!(genericSuperType instanceof ParameterizedType)) {
            throw new RuntimeException("泛型读取异常");
        }
        Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();
        Type actualTypeParam = actualTypeParams[0];
        if (actualTypeParam instanceof ParameterizedType) {
            actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
        }
        if (actualTypeParam instanceof Class) {
            requestClass = (Class<T>) actualTypeParam;
        }
        System.out.println(this.getClass().getName() + " -> " + requestClass.getName());
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
    public T create(String url) {
        if (null != requester) {
            return requester.create(url);
        }
        throw new RuntimeException("非代理请求器必须实现自身执行逻辑");
    }

    @Override
    public T create(String url, Object userData) {
        if (null != requester) {
            return requester.create(url, userData);
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
