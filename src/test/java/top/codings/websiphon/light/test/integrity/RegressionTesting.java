package top.codings.websiphon.light.test.integrity;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.config.RequesterConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.function.handler.AbstractResponseHandler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.CombineRequester;
import top.codings.websiphon.light.requester.support.NettyRequester;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Slf4j
public class RegressionTesting {
    public static void main(String[] args) throws Exception {
//        demo();
//        test1();
        CombineRequester requester = new NettyRequester(RequesterConfig.builder()
                .connectTimeoutMillis(300000)
                .ignoreSslError(true)
                .maxContentLength(Integer.MAX_VALUE)
                .idleTimeMillis(300000)
//                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080)))
                .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                .build());
        IResponseHandler responseHandler = new TestResponseHandler();
        ICrawler crawler1 = createCrawler("1号", responseHandler, requester);
        crawler1.startup().whenCompleteAsync((crawler, throwable) -> {
            if (throwable != null) {
                log.error("爬虫启动失败", throwable.getCause());
            } else {
                log.debug("[{}]爬虫启动", crawler.config().getName());
                crawler.push(
                        "https://www.baidu.com/"
//                        "https://video.twimg.com/ext_tw_video/1295151252536401920/pu/vid/640x352/wpx5Lo0lKRax12hV.mp4?tag=10"
//                        "https://video.twimg.com/ext_tw_video/1299719026067808257/pu/pl/BAQ392kyqXKXAlqm.m3u8?tag=10",
//                        "https://www.google.com.hk",
//                        ,new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080))
                );
                crawler.push("https://www.baidu.com/?t=1");
            }
        });
        /*ICrawler crawler2 = createCrawler("2号", responseHandler, requester);
        crawler2.startup().whenCompleteAsync((crawler, throwable) -> {
            if (throwable != null) {
                log.error("爬虫启动失败", throwable.getCause());
            } else {
                log.debug("[{}]爬虫启动", crawler.config().getName());
                crawler.push("https://www.baidu.com");
                crawler.push("https://www.baidu.com");
                crawler.push("https://www.baidu.com");
            }
        });

        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
                log.debug("开始关机");
                crawler1.shutdown();
                crawler2.shutdown();
            } catch (Exception e) {

            }
        }).start();*/
    }

    private static ICrawler createCrawler(String name, IResponseHandler responseHandler, CombineRequester requester) {
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name(name)
                        .maxConcurrentProcessing(2)
                        .build(),
                responseHandler,
                requester
        )
                .wrapBy(new FakeCrawler())
                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler(5, 2000, 0.7f, (request, c) -> {
                    log.debug("请求任务超时 -> {}", request.getUri().toString());
                }));
        return crawler;
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
                .wrapBy(new RateLimitCrawler(20, 30000 * 4, 0.95f,
                        (iRequest, c) -> System.out.println("超时弹出")));
        crawler.startup().thenAcceptAsync(c -> {
            System.out.println("爬虫已启动");
            for (int i = 0; i < 1; i++) {
                c.push("https://video.twimg.com/ext_tw_video/1299719026067808257/pu/pl/BAQ392kyqXKXAlqm.m3u8?tag=10"
//                        ,Proxy.NO_PROXY
                        , new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080))
                        , "为师大功已成"
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
