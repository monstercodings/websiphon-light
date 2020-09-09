package top.codings.websiphon.light.crawler.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.ComponentCloseAware;
import top.codings.websiphon.light.function.ComponentInitAware;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.CombineRequester;
import top.codings.websiphon.light.requester.support.NettyRequester;

import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class BaseCrawler extends CombineCrawler {
    private IResponseHandler responseHandler;
    private volatile boolean stop = true;
    private volatile boolean begin;

    public BaseCrawler(CrawlerConfig config) {
        this(config, null);
    }

    public BaseCrawler(IResponseHandler responseHandler) {
        this(null, responseHandler);
    }

    public BaseCrawler(CrawlerConfig config, IResponseHandler responseHandler) {
        this(config, responseHandler, null);
    }

    public BaseCrawler(CrawlerConfig config, IResponseHandler responseHandler, CombineRequester requester) {
        if (config == null) {
            config = CrawlerConfig.builder().build();
        }
        if (config.getMaxConcurrentProcessing() <= 0) {
            config.setMaxConcurrentProcessing(Runtime.getRuntime().availableProcessors() + 1);
        }
        if (null == requester) {
            if (StringUtils.isBlank(config.getRequesterClass())) {
                requester = new NettyRequester();
            } else {
                CombineRequester combineRequester = (CombineRequester) IRequester
                        .newBuilder(config)
                        .responseHandler(responseHandler)
                        .build();
                requester = combineRequester;
            }
        } else {
            requester.setResponseHandler(responseHandler);
            if (requester.getStrategy() == null) {
                requester.setStrategy(config.getNetworkErrorStrategy());
            }
        }
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
            requester.setResponseHandler(responseHandler);
        }
        this.responseHandler = responseHandler;
        this.config = config;
        setRequester(requester);
    }

    @Override
    public void push(IRequest request) {
        getRequester().execute(request);
    }

    @Override
    public void push(String url) {
        push(url, null, null);
    }

    @Override
    public void push(String url, Proxy proxy) {
        push(url, proxy, null);
    }

    @Override
    public void push(String url, Object userData) {
        push(url, null, userData);
    }

    @Override
    public void push(String url, Proxy proxy, Object userData) {
        IRequest request = getRequester().create(url, userData);
        request.setProxy(proxy);
        push(request);
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
    public CompletableFuture<? extends ICrawler> startup() {
        ExecutorService exe = Executors.newSingleThreadExecutor();
        return CompletableFuture.supplyAsync(() -> {
            if (!stop) {
                throw new FrameworkException("爬虫已启动");
            }
            synchronized (this) {
                if (begin) {
                    throw new FrameworkException("爬虫正在执行启动/关闭操作，请勿重复执行");
                }
                begin = true;
            }
            if (responseHandler instanceof ComponentInitAware) {
                // 启动响应处理器
                try {
                    ((ComponentInitAware) responseHandler).init(this);
                } catch (FrameworkException e) {
                    throw e;
                } catch (Exception e) {
                    throw new FrameworkException("初始化响应管理器失败", e);
                }
            }
            IRequester requester = getRequester();
            if (requester instanceof ComponentInitAware) {
                try {
                    ((ComponentInitAware) requester).init(this);
                } catch (FrameworkException e) {
                    throw e;
                } catch (Exception e) {
                    throw new FrameworkException("初始化请求器失败", e);
                }
            }
            return this;
        }, exe).whenCompleteAsync((baseCrawler, throwable) -> {
            exe.shutdownNow();
            stop = begin = false;
            if (throwable != null) {
                try {
                    shutdown().get();
                } catch (Exception e) {
                }
            }
        });
    }

    @Override
    public CompletableFuture<ICrawler> shutdown() {
        return CompletableFuture.supplyAsync(() -> {
            if (stop) {
                throw new FrameworkException("爬虫已关闭");
            }
            synchronized (this) {
                if (begin) {
                    throw new FrameworkException("爬虫正在执行启动/关闭操作，请勿重复执行");
                }
                begin = true;
            }
            IRequester requester = getRequester();
            if (requester != null && requester instanceof ComponentCloseAware) {
                try {
                    ((ComponentCloseAware) requester).close();
                } catch (FrameworkException e) {
                    throw e;
                } catch (Exception e) {
                    throw new FrameworkException("关闭请求器失败", e);
                }
            }
            if (null != responseHandler && responseHandler instanceof ComponentCloseAware) {
                try {
                    ((ComponentCloseAware) responseHandler).close();
                } catch (FrameworkException e) {
                    throw e;
                } catch (Exception e) {
                    throw new FrameworkException("关闭响应管理器失败", e);
                }
            }
            Optional.ofNullable(config.getShutdownHook()).ifPresent(action -> action.accept(this.wrapper()));
            stop = true;
            begin = false;
            return this.wrapper();
        });

    }
}
