package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.ComponentFinishAware;
import top.codings.websiphon.light.requester.IRequest;

/**
 * 异步队列处理器
 */
public interface QueueResponseHandler<T extends IRequest, C extends ICrawler> extends IResponseHandler<T>, ComponentFinishAware<C> {
    boolean isBusy();
}
