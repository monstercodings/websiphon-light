package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.RateLimitableCrawler;
import top.codings.websiphon.light.requester.support.CombineRequester;
import top.codings.websiphon.light.requester.support.RateLimitRequester;

public class RateLimitCrawler extends CombineCrawler implements RateLimitableCrawler {
    @Override
    protected void doProxy() {
        CombineRequester oldRequester = getRequester();
        RateLimitRequester requester = new RateLimitRequester(
                oldRequester,
                config.getMaxNetworkConcurrency()
        );
        requester.setCrawler(this);
        setRequester(requester);
    }
}
