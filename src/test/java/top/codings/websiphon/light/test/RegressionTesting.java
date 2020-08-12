package top.codings.websiphon.light.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.ApacheRequester;
import top.codings.websiphon.light.requester.support.BuiltinRequester;
import top.codings.websiphon.light.requester.support.NettyRequester;
import top.codings.websiphon.light.test.dependent.DoNothingRequester;
import top.codings.websiphon.light.test.dependent.TestResponseHandler;

public class RegressionTesting {
    @Test
    public void test1() throws InterruptedException {
        QpsDataStat stat = new QpsDataStat(0);
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name("test")
                        .version("0.0.1")
                        .sync(false)
//                        .requesterClass(DoNothingRequester.class.getName())
                        .requesterClass(NettyRequester.class.getName())
//                        .requesterClass(ApacheRequester.class.getName())
//                        .requesterClass(BuiltinRequester.class.getName())
                        .responseHandlerImplClass(TestResponseHandler.class.getName())
                        .maxNetworkConcurrency(5)
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .build())
                .wrapBy(new StatCrawler<>(stat))
                .wrapBy(new FakeCrawler())
                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler(0.95f, (iRequest, c) -> {
                    System.out.println("超时弹出");
                }));
        crawler.startup();
        crawler.push("http://localhost:8080/header");
        crawler.push("http://localhost:8080/header");
        while (crawler.isBusy()) {
            Thread.onSpinWait();
        }
    }
}
