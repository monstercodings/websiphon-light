package top.codings.websiphon.light.crawler.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.CombineRequester;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        if (null == responseHandler && StringUtils.isNotBlank(config.getResponseHandlerImplClass())) {
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
        if (null != responseHandler) {
            responseHandler.setConfig(config);
        }
        this.responseHandler = responseHandler;
        if (null == requester) {
            CombineRequester combineRequester = (CombineRequester) IRequester
                    .newBuilder(config)
                    .responseHandler(responseHandler)
                    .build();
            setRequester(combineRequester);
        }
    }

    @Override
    public void push(IRequest request) {
        getRequester().execute(request);
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
    public CompletableFuture<ICrawler> startup() {
        CompletableFuture<ICrawler> completableFuture = CompletableFuture.supplyAsync(() -> this);
        if (responseHandler instanceof QueueResponseHandler) {
            // 启动响应处理器
            completableFuture = completableFuture.thenCombineAsync(((QueueResponseHandler) responseHandler).startup(this), (crawler, iResponseHandler) -> crawler);
        }
        return completableFuture.thenCombineAsync(getRequester().init(), (crawler, o) -> crawler);
    }

    @Override
    public CompletableFuture<ICrawler> shutdown() {
        CompletableFuture<ICrawler> completableFuture = CompletableFuture.completedFuture(this.wrapper());
        boolean force = true;
        IRequester requester = getRequester();
        if (requester != null) {
            completableFuture = completableFuture.thenCombineAsync(requester.shutdown(force), (crawler, o) -> crawler);
        }
        if (null != responseHandler) {
            if (responseHandler instanceof QueueResponseHandler) {
                completableFuture = completableFuture.thenCombineAsync(((QueueResponseHandler) responseHandler).shutdown(force), (crawler, iResponseHandler) -> crawler);
            }
        }
        Optional.ofNullable(config.getShutdownHook()).ifPresent(action -> action.accept(this.wrapper()));
        return completableFuture;
    }
}
