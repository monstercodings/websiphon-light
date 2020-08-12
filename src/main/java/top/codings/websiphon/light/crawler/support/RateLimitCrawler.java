package top.codings.websiphon.light.crawler.support;

import lombok.NoArgsConstructor;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.RateLimitableCrawler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.CombineRequester;
import top.codings.websiphon.light.requester.support.RateLimitRequester;

import java.util.function.BiConsumer;

@NoArgsConstructor
public class RateLimitCrawler extends CombineCrawler implements RateLimitableCrawler {
    private final static float DEFAULT_LIMIT_MEMORY = 0.7f;
    private float limitMemory = DEFAULT_LIMIT_MEMORY;
    private BiConsumer<IRequest, ICrawler> timeoutHandler;

    public RateLimitCrawler(float limitMemory) {
        this(limitMemory, null);
    }

    public RateLimitCrawler(BiConsumer<IRequest, ICrawler> timeoutHandler) {
        this(DEFAULT_LIMIT_MEMORY, timeoutHandler);
    }

    public RateLimitCrawler(float limitMemory, BiConsumer<IRequest, ICrawler> timeoutHandler) {
        if (limitMemory > 1f) {
            throw new RuntimeException("内存限制阈值只能为[0,1]");
        }
        this.limitMemory = limitMemory;
        this.timeoutHandler = timeoutHandler;
    }

    @Override
    protected void doProxy() {
        CombineRequester oldRequester = getRequester();
        RateLimitRequester requester = new RateLimitRequester(
                oldRequester,
                config.getMaxNetworkConcurrency(),
                timeoutHandler
        );
        requester.setCrawler(this);
        requester.setLimitMemory(limitMemory);
        setRequester(requester);
    }

    @Override
    public BiConsumer<IRequest, ICrawler> timeoutHandler() {
        return timeoutHandler;
    }
}
