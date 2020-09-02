package top.codings.websiphon.light.function.processor;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.StopHandlErrorException;
import top.codings.websiphon.light.requester.IRequest;

public interface ProcessErrorAware {
    void doOnError(IRequest request, Throwable throwable, ICrawler crawler) throws StopHandlErrorException;
}
