package top.codings.websiphon.light.test;

import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.config.RequesterConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.ApacheRequester;
import top.codings.websiphon.light.requester.support.BuiltinRequester;
import top.codings.websiphon.light.requester.support.NettyRequester;
import top.codings.websiphon.light.test.dependent.DoNothingRequester;
import top.codings.websiphon.light.test.dependent.TestResponseHandler;

public class RegressionTesting {
    public static void main(String[] args) throws Exception {
        test1();
    }

    public static void test1() throws InterruptedException {
        QpsDataStat stat = new QpsDataStat(0);
        ICrawler crawler = new BaseCrawler(
                CrawlerConfig.builder()
                        .name("test")
                        .version("0.0.1")
//                        .requesterClass(DoNothingRequester.class.getName())
                        .requesterClass(NettyRequester.class.getName())
//                        .requesterClass(ApacheRequester.class.getName())
//                        .requesterClass(BuiltinRequester.class.getName())
                        .responseHandlerImplClass(TestResponseHandler.class.getName())
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .shutdownHook(c -> System.out.println(c.config().getName() + " | 爬虫马上就要关闭啦~~~"))
                        .build(),
                null
                /*new BuiltinRequester(RequesterConfig.builder()
                        .connectTimeoutMillis(30000)
                        .idleTimeMillis(30000)
                        .redirect(true)
                        .ignoreSslError(true)
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .maxContentLength(Integer.MAX_VALUE)
                        .build())*/
                /*new ApacheRequester(RequesterConfig.builder()
                        .connectTimeoutMillis(30000)
                        .idleTimeMillis(30000)
                        .redirect(true)
                        .ignoreSslError(true)
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .maxContentLength(Integer.MAX_VALUE)
                        .build())*/
                /*new NettyRequester(RequesterConfig.builder()
                        .connectTimeoutMillis(30000)
                        .ignoreSslError(true)
                        .maxContentLength(1024 * 512)
                        .build())*/
//                new DoNothingRequester()
        )
                .wrapBy(new StatCrawler<>(stat))
                .wrapBy(new FakeCrawler())
                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler(5, 30000, 0.95f,
                        (iRequest, c) -> System.out.println("超时弹出")));
        crawler.startup().thenAcceptAsync(c -> {
            System.out.println("爬虫已启动");
            c.push("https://vdash.codings.top:7921");
//            c.push("http://localhost:8080/header");
        });
//        Thread.currentThread().join();
        /*while (crawler.isBusy()) {
            Thread.onSpinWait();
        }
        crawler.shutdown();*/
    }
}
