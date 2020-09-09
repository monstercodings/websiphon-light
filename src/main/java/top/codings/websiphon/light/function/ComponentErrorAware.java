package top.codings.websiphon.light.function;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.StopHandlErrorException;
import top.codings.websiphon.light.requester.IRequest;

public interface ComponentErrorAware {
    void doOnError(Throwable throwable, IRequest request, ICrawler crawler) throws Exception;
}
