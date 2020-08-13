package top.codings.websiphon.light.test;

import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class TestTotal {
    private static AtomicBoolean keep = new AtomicBoolean(true);

    public static void main(String[] args) throws Exception {
        TestTotal test = new TestTotal();
        ICrawler crawler = test.startup();

//        test.createHttpServer();
        while (keep.get()) {
            for (int i = 0; i < 30; i++) {
                /*IRequest request = new BuiltinRequest(HttpRequest.newBuilder()
                        .uri(URI.create("http://192.168.1.117:8080/test"))
                        .build());*/
                /*crawler.push(new BuiltinRequest(HttpRequest.newBuilder()
                        .uri(URI.create("https://www.baidu.com"))
                        .build()));*/
//                crawler.push(new ApacheRequest(new HttpGet("http://192.168.0.113:8080/test")));
//                crawler.push(new ApacheRequest(new HttpGet("https://www.baidu.com")));
                crawler.push("https://www.baidu.com/?a=" + i);
            }
            break;
        }
//        System.out.println("停止请求");
    }

    private void createHttpServer() throws IOException {
        byte[] content = "ok".getBytes("utf-8");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/stop", exchange -> {
            try {
                keep.set(false);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, content.length);
                OutputStream os = exchange.getResponseBody();
                os.write(content);
                os.flush();
            } finally {
                exchange.close();
            }
        });
        server.createContext("/gc", exchange -> {
            try {
                System.gc();
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
        QpsDataStat stat = new QpsDataStat(0);
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name("我的测试爬虫")
                        .version("0.0.1")
                        .maxNetworkConcurrency(100)
                        .responseHandlerImplClass("top.codings.websiphon.light.test.dependent.TestResponseHandler")
//                        .requesterClass("top.codings.websiphon.light.requester.support.BuiltinRequester")
//                        .requesterClass("top.codings.websiphon.light.requester.support.ApacheRequester")
                        .requesterClass("top.codings.websiphon.light.requester.support.NettyRequester")
                        .shutdownHook(spider -> log.debug("[{}] 爬虫关闭", spider.config().getName()))
                        .build())
                .wrapBy(new StatCrawler<>(stat, true))
                .wrapBy(new FakeCrawler())
                .wrapBy(new FiltrateCrawler())
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
