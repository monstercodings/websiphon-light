package top.codings.websiphon.light.crawler.support;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.CombineRequester;

@Slf4j
public class BaseCrawler extends CombineCrawler {
    private IResponseHandler responseHandler;

    public BaseCrawler(CrawlerConfig config) {
        this.config = config;
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
        /*if (responseHandler instanceof QueueResponseHandler) {
            ((QueueResponseHandler) responseHandler).setCrawler(this);
        }*/
        responseHandler.setConfig(config);
        setRequester((CombineRequester) IRequester
                .newBuilder(config)
                .responseHandler(responseHandler)
                .build());
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
        return getRequester().isBusy() | responseHandler.isBusy();
    }

    @Override
    public void startup() {
        // 启动响应处理器
        responseHandler.startup(this);
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
