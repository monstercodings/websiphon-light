package top.codings.websiphon.light.crawler.support;

import lombok.NoArgsConstructor;
import lombok.Setter;
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
    @Setter
    private float limitMemory;
    private final static int DEFAULT_MAX_NETWORK_CONCURRENCY = 5;
    @Setter
    private int maxNetworkConcurrency;
    private final static int DEFAULT_TASK_TIMEOUT_MILLIS = 60000;
    @Setter
    private int taskTimeoutMillis;
    @Setter
    private BiConsumer<IRequest, ICrawler> timeoutHandler;

    public RateLimitCrawler(int maxNetworkConcurrency) {
        this(maxNetworkConcurrency, DEFAULT_TASK_TIMEOUT_MILLIS, DEFAULT_LIMIT_MEMORY, null);
    }

    public RateLimitCrawler(float limitMemory) {
        this(DEFAULT_MAX_NETWORK_CONCURRENCY, DEFAULT_TASK_TIMEOUT_MILLIS, limitMemory, null);
    }

    public RateLimitCrawler(int maxNetworkConcurrency, int taskTimeoutMillis, float limitMemory) {
        this(maxNetworkConcurrency, taskTimeoutMillis, limitMemory, null);
    }

    public RateLimitCrawler(BiConsumer<IRequest, ICrawler> timeoutHandler) {
        this(DEFAULT_MAX_NETWORK_CONCURRENCY, DEFAULT_TASK_TIMEOUT_MILLIS, DEFAULT_LIMIT_MEMORY, timeoutHandler);
    }

    public RateLimitCrawler(int maxNetworkConcurrency, int taskTimeoutMillis, float limitMemory, BiConsumer<IRequest, ICrawler> timeoutHandler) {
        this.maxNetworkConcurrency = maxNetworkConcurrency;
        if (limitMemory > 1f) {
            throw new RuntimeException("内存限制阈值只能为[0,1]");
        }
        this.taskTimeoutMillis = taskTimeoutMillis;
        this.limitMemory = limitMemory;
        this.timeoutHandler = timeoutHandler;
    }

    @Override
    protected void doProxy() {
        CombineRequester oldRequester = getRequester();
        RateLimitRequester requester = new RateLimitRequester(
                oldRequester,
                maxNetworkConcurrency,
                taskTimeoutMillis,
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
