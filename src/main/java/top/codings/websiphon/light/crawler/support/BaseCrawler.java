package top.codings.websiphon.light.crawler.support;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.CombineRequester;

@Slf4j
public class BaseCrawler extends CombineCrawler {
    private IResponseHandler responseHandler;

    public BaseCrawler(CrawlerConfig config) {
        this(config, null);
    }

    public BaseCrawler(CrawlerConfig config, IResponseHandler responseHandler) {
        this(config, responseHandler, null);
    }

    public BaseCrawler(CrawlerConfig config, IResponseHandler responseHandler, IRequester requester) {
        if (config.getMaxConcurrentProcessing() <= 0) {
            config.setMaxConcurrentProcessing(Runtime.getRuntime().availableProcessors() + 1);
        }
        this.config = config;
        if (null == responseHandler) {
            try {
                // 初始化响应处理器
                responseHandler = (IResponseHandler) Class.forName(
                        config.getResponseHandlerImplClass(),
                        true,
                        config.getClassLoader() == null ? ClassLoader.getSystemClassLoader() : config.getClassLoader()
                ).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("初始化响应处理器失败", e);
            }
        }
        responseHandler.setConfig(config);
        this.responseHandler = responseHandler;
        if (null == requester) {
            CombineRequester combineRequester = (CombineRequester) IRequester
                    .newBuilder(config)
                    .responseHandler(responseHandler)
                    .build();
            if (config.getNetworkErrorStrategy() != null) {
                combineRequester.setStrategy(config.getNetworkErrorStrategy());
            }
            setRequester(combineRequester);
        }
    }

    @Override
    public void push(IRequest request) {
        getRequester().executeAsync(request);
    }

    @Override
    public void push(String url) {
        push(url, null);
    }

    @Override
    public void push(String url, Object userData) {
        push(getRequester().create(url, userData));
    }

    @Override
    public boolean isBusy() {
        boolean isBusy = (responseHandler instanceof QueueResponseHandler) ? ((QueueResponseHandler) responseHandler).isBusy() : false;
        return getRequester().isBusy() | isBusy;
    }

    @Override
    public void startup() {
        if (responseHandler instanceof QueueResponseHandler) {
            // 启动响应处理器
            ((QueueResponseHandler) responseHandler).startup(this);
        }
        // 初始化请求器，并使用装饰器模式增强内建请求器
        /*requester = new DistinctRequester(
                new RateLimitRequester(
                        new FakeRequester(
                                (AsyncRequester) IRequester
                                        .newBuilder(true)
                                        .responseHandler(responseHandler)
                                        .build()
                        ),
                        config.getMaxNetworkConcurrency()
                ),
                cleanableFilter
        );*/
        // 请求器初始化
        getRequester().init();
    }

    @Override
    public void shutdown() {
        IRequester requester = getRequester();
        if (requester != null) {
            requester.shutdown(true);
        }
    }
}
