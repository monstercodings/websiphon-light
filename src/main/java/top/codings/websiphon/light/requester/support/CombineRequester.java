package top.codings.websiphon.light.requester.support;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.ComponentCloseAware;
import top.codings.websiphon.light.function.ComponentInitAware;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class CombineRequester<T extends IRequest> implements IRequester<T>, ComponentInitAware<ICrawler>, ComponentCloseAware {
    private static final FrameworkException NONE_IMPLEMENT=new FrameworkException("非代理请求器必须实现自身执行逻辑");
    private AtomicInteger initIndex = new AtomicInteger(0);
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
    public final void init(ICrawler crawler) throws Exception {
        if (initIndex.compareAndSet(0, 1)) {
            syncForInit(crawler, 0);
            if (null != requester) {
                requester.init(crawler);
                return;
            }
            return;
        }
        if (getClass().getDeclaredAnnotation(Shared.class) == null) {
            throw new FrameworkException(String.format(
                    "[%s]为非共享组件，如需提供给多个爬虫使用，请使用@Shared对该请求器进行修饰",
                    getClass().getName()
            ));
        }
        syncForInit(crawler, initIndex.getAndIncrement());
        if (null != requester) {
            requester.init(crawler);
            return;
        }
    }

    /**
     * 使用锁机制确保请求器的初始化操作能在真正请求发起之前完成
     *
     * @param crawler
     * @param index
     * @throws Exception
     */
    private void syncForInit(ICrawler crawler, int index) throws Exception {
        synchronized (this) {
            init(crawler, index);
        }
    }

    protected void init(ICrawler crawler, int index) throws Exception {
        if (null == requester) {
            throw NONE_IMPLEMENT;
        }
    }

    @Override
    public CompletableFuture<T> execute(T request) {
        if (null != requester) {
            return requester.execute(request);
        }
        throw NONE_IMPLEMENT;
    }

    @Override
    public T create(String url) {
        if (null != requester) {
            return requester.create(url);
        }
        throw NONE_IMPLEMENT;
    }

    @Override
    public T create(String url, Object userData) {
        if (null != requester) {
            return requester.create(url, userData);
        }
        throw NONE_IMPLEMENT;
    }

    @Override
    public final void close() throws Exception {
        int index = initIndex.decrementAndGet();
        if (index < 0) {
            return;
        }
        close(index);
        if (null != requester) {
            requester.close();
            return;
        }
    }

    protected void close(int index) throws Exception {
        if (null == requester) {
            throw NONE_IMPLEMENT;
        }
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
        throw NONE_IMPLEMENT;
    }

    @Override
    public IResponseHandler getResponseHandler() {
        if (null != requester) {
            return requester.getResponseHandler();
        }
        throw NONE_IMPLEMENT;
    }

    public final NetworkErrorStrategy getStrategy() {
        return strategy;
    }

    public final void setStrategy(NetworkErrorStrategy strategy) {
        this.strategy = strategy;
    }
}
