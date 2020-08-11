package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.requester.IRequest;

import java.util.function.Consumer;

/**
 * 可限制网络并发请求的爬虫
 */
public interface RateLimitableCrawler {
    Consumer<IRequest> timeoutHandler();
}
