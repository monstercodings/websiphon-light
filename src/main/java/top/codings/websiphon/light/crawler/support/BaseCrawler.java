package top.codings.websiphon.light.crawler.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.CombineRequester;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class BaseCrawler extends CombineCrawler {
    private IResponseHandler responseHandler;
    private volatile boolean stop = true;
    private volatile boolean begin;

    public BaseCrawler(CrawlerConfig config) {
        this(config, null);
    }

    public BaseCrawler(CrawlerConfig config, IResponseHandler responseHandler) {
        this(config, responseHandler, null);
    }

    public BaseCrawler(CrawlerConfig config, IResponseHandler responseHandler, CombineRequester requester) {
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
            requester = combineRequester;
        } else {
            requester.setResponseHandler(responseHandler);
            if (requester.getStrategy() == null) {
                requester.setStrategy(config.getNetworkErrorStrategy());
            }
        }
        setRequester(requester);
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
    public boolean isStop() {
        return stop;
    }

    @Override
    public boolean isRunning() {
        return !isStop();
    }

    @Override
    public CompletableFuture<ICrawler> startup() {
        if (begin) {
            throw new FrameworkException("爬虫正在执行启动/关闭操作，请勿重复执行");
        }
        synchronized (this) {
            if (begin) {
                throw new FrameworkException("爬虫正在执行启动/关闭操作，请勿重复执行");
            }
            begin = true;
        }
        CompletableFuture<ICrawler> completableFuture = CompletableFuture.completedFuture(this);
        if (responseHandler instanceof QueueResponseHandler) {
            // 启动响应处理器
            completableFuture = completableFuture.thenCombineAsync(((QueueResponseHandler) responseHandler).startup(this), (crawler, iResponseHandler) -> crawler);
        }
        return completableFuture
                .thenCombineAsync(getRequester().init(), (crawler, o) -> crawler)
                .whenCompleteAsync((o, o2) -> stop = begin = false);
    }

    @Override
    public CompletableFuture<ICrawler> shutdown() {
        if (begin) {
            throw new FrameworkException("爬虫正在执行启动/关闭操作，请勿重复执行");
        }
        synchronized (this) {
            if (begin) {
                throw new FrameworkException("爬虫正在执行启动/关闭操作，请勿重复执行");
            }
            begin = true;
        }
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
        CountDownLatch latch = new CountDownLatch(1);
        completableFuture.whenCompleteAsync((crawler, throwable) -> latch.countDown());
        // TODO 未来看看有没有更好的解决方案
        Runnable runnable = () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Optional.ofNullable(config.getShutdownHook()).ifPresent(action -> action.accept(this.wrapper()));
        };
        if (Thread.currentThread().isInterrupted()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }

        stop = true;
        begin = false;
        return completableFuture;
    }
}
