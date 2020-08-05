package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.requester.support.BuiltinRequest;

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
     * @param request
     */
    void push(BuiltinRequest request);

    /**
     * 查看爬虫是否空闲
     * @return
     */
    boolean isBusy();
}
