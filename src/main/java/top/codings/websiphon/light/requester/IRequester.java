package top.codings.websiphon.light.requester;

import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.manager.QueueResponseHandler;

import java.util.concurrent.CompletableFuture;

public interface IRequester<T extends IRequest> {
    /**
     * 初始化请求器
     */
    void init();

    /**
     * 发起异步网络请求
     *
     * @param request 请求对象
     * @return
     */
    CompletableFuture<T> executeAsync(T request);

    T create(String url);

    T create(String url, Object userData);

    /**
     * 关闭请求器
     *
     * @param force
     */
    void shutdown(boolean force);

    /**
     * 查看请求器是否繁忙
     *
     * @return
     */
    boolean isBusy();

    IResponseHandler getResponseHandler();

    static RequesterBuilder newBuilder(CrawlerConfig config) {
        return new RequesterBuilder(config);
    }

    class RequesterBuilder {
        private IRequester requester;
        private boolean sync;

        public RequesterBuilder(CrawlerConfig config) {
            this.sync = config.isSync();
            if (sync) {
                throw new RuntimeException("同步模式暂未支持");
            } else {
//                requester = new BuiltinRequester();
//                requester = new ApacheRequester();
                try {
                    String className = config.getRequesterClass();
                    if (StringUtils.isBlank(className)) {
                        throw new IllegalArgumentException("请设定请求器全限定类名");
                    }
                    requester = (IRequester) Class.forName(
                            className,
                            true,
                            config.getClassLoader() == null ? ClassLoader.getSystemClassLoader() : config.getClassLoader()
                    ).getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("初始化请求器失败", e);
                }
            }
        }

        public RequesterBuilder responseHandler(IResponseHandler responseHandler) {
            if (sync) {
                SyncRequester syncRequester = (SyncRequester) requester;
                syncRequester.setResponseHandler(responseHandler);
            } else {
                AsyncRequester asyncRequester = (AsyncRequester) requester;
                asyncRequester.setResponseHandler((QueueResponseHandler) responseHandler);
            }
            return this;
        }

        public IRequester build() {
            return requester;
        }
    }
}
