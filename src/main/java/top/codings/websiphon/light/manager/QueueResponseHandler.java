package top.codings.websiphon.light.manager;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

/**
 * 异步队列处理器
 */
public interface QueueResponseHandler extends IResponseHandler{
    boolean push(IRequest request);

    void whenFinish(ICrawler crawler);

//    void setCrawler(ICrawler crawler);
}
