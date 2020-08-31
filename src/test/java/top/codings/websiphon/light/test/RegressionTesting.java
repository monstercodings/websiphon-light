package top.codings.websiphon.light.test;

import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.config.RequesterConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.function.handler.AbstractResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.NettyRequester;
import top.codings.websiphon.light.test.dependent.TestResponseHandler;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class RegressionTesting {
    public static void main(String[] args) throws Exception {
//        demo();
        test1();
    }

    public static void demo() throws Exception {
        ICrawler crawler = new BaseCrawler(
                new AbstractResponseHandler() {
                    @Override
                    public void handle(IRequest request) {
                        // 此处写响应处理逻辑
                    }
                }
        );
        crawler.startup();
        crawler.push("https://www.baidu111.com");
        // 主动关闭爬虫
        // crawler.shutdown();
    }

    public static void test1() throws InterruptedException {
        QpsDataStat stat = new QpsDataStat(0);
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name("test")
                        .version("0.0.1")
//                        .requesterClass(DoNothingRequester.class.getName())
//                        .requesterClass(NettyRequester.class.getName())
//                        .requesterClass(ApacheRequester.class.getName())
//                        .requesterClass(BuiltinRequester.class.getName())
                        .responseHandlerImplClass(TestResponseHandler.class.getName())
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .shutdownHook(c -> System.out.println(c.config().getName() + " | 爬虫马上就要关闭啦~~~"))
                        .build(),
                null,
                /*new BuiltinRequester(RequesterConfig.builder()
                        .connectTimeoutMillis(30000)
                        .idleTimeMillis(30000)
                        .redirect(true)
                        .ignoreSslError(true)
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .maxContentLength(Integer.MAX_VALUE)
                        .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080)))
                        .build())*/
                /*new ApacheRequester(RequesterConfig.builder()
                        .connectTimeoutMillis(30000)
                        .idleTimeMillis(30000)
                        .redirect(true)
                        .ignoreSslError(true)
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .maxContentLength(Integer.MAX_VALUE)
                        .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080)))
                        .build())*/
                new NettyRequester(RequesterConfig.builder()
                        .connectTimeoutMillis(30000)
                        .ignoreSslError(true)
                        .maxContentLength(1024 * 512)
                        .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)))
                        .build())
//                new DoNothingRequester()
        )
                .wrapBy(new StatCrawler<>(stat))
                .wrapBy(new FakeCrawler())
                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler(5, 30000, 0.95f,
                        (iRequest, c) -> System.out.println("超时弹出")));
        crawler.startup().thenAcceptAsync(c -> {
            System.out.println("爬虫已启动");
            for (int i = 0; i < 1; i++) {
                c.push("https://video.twimg.com/ext_tw_video/1299719026067808257/pu/pl/BAQ392kyqXKXAlqm.m3u8?tag=10"
//                        ,Proxy.NO_PROXY
                        ,new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080))
                );
            }
//            c.push("http://localhost:8080/header");
        });
//        Thread.currentThread().join();
        /*while (crawler.isBusy()) {
            Thread.onSpinWait();
        }
        crawler.shutdown();*/
    }
}
