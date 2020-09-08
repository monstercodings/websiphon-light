package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.crawler.ICrawler;

/**
 * 异步队列处理器
 */
public interface QueueResponseHandler extends IResponseHandler {
    boolean isBusy();

    void whenFinish(ICrawler crawler);

}
