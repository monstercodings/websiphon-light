package top.codings.websiphon.light.test;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TotalTest {

    public static void main(String[] args) throws Exception {
        TotalTest test = new TotalTest();
        ICrawler crawler = test.startup();
        test.createHttpServer();
        for (; ; ) {
            for (int i = 0; i < 10000; i++) {
                crawler.push(new BuiltinRequest(HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:8080/test"))
                        .build()));
            }
            Thread.sleep(100);
        }
    }

    private void createHttpServer() throws IOException {
        byte[] content = "hello".getBytes("utf-8");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/test", exchange -> {
            try {
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, content.length);
                OutputStream os = exchange.getResponseBody();
                os.write(content);
                os.flush();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    public ICrawler startup() throws Exception {
        QpsDataStat stat = new QpsDataStat(1000);
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name("我的测试爬虫")
                        .version("0.0.1")
                        .maxNetworkConcurrency(100)
                        .maxConcurrentProcessing(Runtime.getRuntime().availableProcessors())
                        .responseHandlerImplClass("top.codings.websiphon.light.test.dependent.TestResponseHandler")
                        .build())
                .wrapBy(new StatCrawler<>(stat, true))
                .wrapBy(new FakeCrawler())
//                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler());
        crawler.startup();
        return crawler;
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
        for (long i = 0l; i < Integer.MAX_VALUE + 1l; i++) {
            try {
                exe.submit(() -> {
                    long c = count.incrementAndGet();
                    TimeUnit.SECONDS.sleep(100);
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
