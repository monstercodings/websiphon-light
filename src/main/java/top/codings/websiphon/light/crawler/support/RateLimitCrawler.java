package top.codings.websiphon.light.crawler.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.RateLimitableCrawler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.CombineRequester;
import top.codings.websiphon.light.requester.support.RateLimitRequester;

import java.util.function.Consumer;

@NoArgsConstructor
public class RateLimitCrawler extends CombineCrawler implements RateLimitableCrawler {
    private Consumer<IRequest> timeoutHandler;

    public RateLimitCrawler(Consumer<IRequest> timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
    }

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

    @Override
    public Consumer<IRequest> timeoutHandler() {
        return timeoutHandler;
    }
}
