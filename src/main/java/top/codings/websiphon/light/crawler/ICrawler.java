package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.requester.IRequest;

public interface ICrawler {
    /**
     * 启动爬虫
     */
    void startup();

    /**
     * 关闭爬虫
     */
    void shutdown();

    /**
     * 将任务推送给爬虫
     *
     * @param request
     */
    void push(IRequest request);

    void push(String url);

    void push(String url, Object userData);

    /**
     * 查看爬虫是否空闲
     *
     * @return
     */
    boolean isBusy();
}
