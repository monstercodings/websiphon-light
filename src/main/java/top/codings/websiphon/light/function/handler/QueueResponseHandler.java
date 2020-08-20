package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.CompletableFuture;

/**
 * 异步队列处理器
 */
public interface QueueResponseHandler extends IResponseHandler{
    CompletableFuture<IResponseHandler> startup(ICrawler crawler);

    CompletableFuture<IResponseHandler> shutdown(boolean force);

    boolean isBusy();

    void whenFinish(ICrawler crawler);

}
