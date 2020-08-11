package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

/**
 * 异步队列处理器
 */
public interface QueueResponseHandler extends IResponseHandler{
    void startup(ICrawler crawler);

    void shutdown(boolean force);

    boolean isBusy();

//    boolean push(IRequest request);

    void whenFinish(ICrawler crawler);

//    void setCrawler(ICrawler crawler);
}
