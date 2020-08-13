package top.codings.websiphon.light.requester;

import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.handler.IResponseHandler;

import java.util.concurrent.CompletableFuture;

public interface IRequester<T extends IRequest> {
    /**
     * 初始化请求器
     */
    CompletableFuture<IRequester> init();

    /**
     * 发起异步网络请求
     *
     * @param request 请求对象
     * @return
     */
    CompletableFuture<T> execute(T request);

    T create(String url);

    T create(String url, Object userData);

    /**
     * 关闭请求器
     *
     * @param force
     */
    CompletableFuture<IRequester> shutdown(boolean force);

    /**
     * 查看请求器是否繁忙
     *
     * @return
     */
    boolean isBusy();

    /**
     * 设置响应处理器
     *
     * @param responseHandler
     */
    void setResponseHandler(IResponseHandler responseHandler);

    /**
     * 获取响应处理器
     *
     * @return
     */
    IResponseHandler getResponseHandler();

    enum NetworkErrorStrategy {
        RESPONSE(),
        DROP(),
    }

    static RequesterBuilder newBuilder(CrawlerConfig config) {
        return new RequesterBuilder(config);
    }

    class RequesterBuilder {
        private CrawlerConfig config;
        private IRequester requester;
        private IResponseHandler responseHandler;

        public RequesterBuilder(CrawlerConfig config) {
            this.config = config;
        }

        public RequesterBuilder responseHandler(IResponseHandler responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }

        public IRequester build() {
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
                throw new FrameworkException("初始化请求器失败", e);
            }
            requester.setResponseHandler(responseHandler);
            return requester;
        }
    }
}
