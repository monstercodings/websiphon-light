package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.requester.IRequest;

import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

public interface ICrawler {
    /**
     * 启动爬虫
     */
    CompletableFuture<ICrawler> startup();

    /**
     * 关闭爬虫
     */
    CompletableFuture<ICrawler> shutdown();

    /**
     * 将任务推送给爬虫
     *
     * @param request
     */
    void push(IRequest request);

    void push(String url);

    void push(String url, Proxy proxy);

    void push(String url, Object userData);

    void push(String url, Proxy proxy, Object userData);

    /**
     * 查看爬虫是否空闲
     *
     * @return
     */
    boolean isBusy();

    boolean isStop();

    boolean isRunning();

    CrawlerConfig config();
}
