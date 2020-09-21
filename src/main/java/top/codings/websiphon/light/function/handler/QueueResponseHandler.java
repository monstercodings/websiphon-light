package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.ComponentFinishAware;
import top.codings.websiphon.light.requester.IRequest;

/**
 * 队列处理器管理接口
 */
public interface QueueResponseHandler<T extends IRequest, C extends ICrawler> extends IResponseHandler<T>, ComponentFinishAware<C> {
    boolean isBusy();

    /**
     * 同步背压，确保有足够的处理器线程应对响应数据
     * 若处理线程已满，则需要阻塞至处理队列完成处理后方可继续下载数据
     */
    void syncBackpress() throws InterruptedException;
}
