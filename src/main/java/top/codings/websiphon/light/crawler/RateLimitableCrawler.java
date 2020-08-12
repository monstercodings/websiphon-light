package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.requester.IRequest;

import java.util.function.BiConsumer;

/**
 * 可限制网络并发请求的爬虫
 */
public interface RateLimitableCrawler {
    BiConsumer<IRequest, ICrawler> timeoutHandler();
}
