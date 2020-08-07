package top.codings.websiphon.light.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TotalTest {

    public static void main(String[] args) throws Exception {
        new TotalTest().test();
    }

    public void test() throws Exception {
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
                .uri(URI.create("http://127.0.0.1:8080/sleep"))
                .build()));
        /*TimeUnit.SECONDS.sleep(1);
        crawler.push(new BuiltinRequest(HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com?a=1"))
                .build()));
        Thread.currentThread().join();*/
    }

    @Test
    public void test1() throws InterruptedException {
        ExecutorService exe = Executors.newSingleThreadExecutor();
        AtomicLong count = new AtomicLong(0);
        for (long i = 0l; i < Integer.MAX_VALUE + 10l; i++) {
            try {
                exe.submit(() -> {
                    long c = count.incrementAndGet();
                    TimeUnit.SECONDS.sleep(10);
                    if (c >= Integer.MAX_VALUE - 3) {
                        System.out.println("数量 -> " + c);
                    }
                    //                TimeUnit.SECONDS.sleep(1);
                    //                System.out.println(String.format("%s", Thread.currentThread().getId()));
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("任务放入完成");
        Thread.currentThread().join();
    }
}
