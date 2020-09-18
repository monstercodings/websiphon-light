package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.StatisticalCrawler;
import top.codings.websiphon.light.requester.IRequest;

/**
 * 具备统计功能的处理器
 * 需要配合具备统计功能的爬虫和请求器来使用
 * 否则单独使用不生效
 */
public abstract class StatResponseHandler<T extends IRequest> extends ChainResponseHandler<T> {
    @Override
    protected void afterHandle(T request, ICrawler crawler, Throwable cause) {
        if (crawler instanceof CombineCrawler) {
            CombineCrawler combineCrawler = (CombineCrawler) crawler;
            combineCrawler.find(StatisticalCrawler.class).ifPresent(statisticalCrawler -> statisticalCrawler.stat().getResponseCountTotal().increment());
        }
    }
}
