package top.codings.websiphon.light.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

import java.net.URI;
import java.net.http.HttpRequest;

public class TotalTest {

    public static void main(String[] args) throws InterruptedException {
        new TotalTest().test();
    }

    @Test
    public void test() throws InterruptedException {
        QpsDataStat stat = new QpsDataStat(30);
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name("我的测试爬虫")
                        .version("0.0.1")
                        .maxNetworkConcurrency(100)
                        .maxConcurrentProcessing(Runtime.getRuntime().availableProcessors())
                        .responseHandlerImplClass("top.codings.websiphon.light.test.dependent.TestResponseHandler")
                        .build())
                .wrapBy(new StatCrawler<>(stat))
                .wrapBy(new FakeCrawler())
                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler());
        crawler.startup();
        crawler.push(new BuiltinRequest(HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com"))
                .build()));
        Thread.currentThread().join();
    }
}
