package top.codings.websiphon.light.test;

import top.codings.websiphon.light.bean.QpsDataStat;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.support.*;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.requester.support.ApacheRequester;
import top.codings.websiphon.light.requester.support.BuiltinRequester;
import top.codings.websiphon.light.requester.support.NettyRequester;
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
                        .sync(false)
//                        .requesterClass(DoNothingRequester.class.getName())
//                        .requesterClass(NettyRequester.class.getName())
//                        .requesterClass(ApacheRequester.class.getName())
                        .requesterClass(BuiltinRequester.class.getName())
                        .responseHandlerImplClass(TestResponseHandler.class.getName())
                        .maxNetworkConcurrency(5)
                        .networkErrorStrategy(IRequester.NetworkErrorStrategy.RESPONSE)
                        .shutdownHook(c -> System.out.println(c.config().getName() + " | 爬虫马上就要关闭啦~~~"))
                        .build())
                .wrapBy(new StatCrawler<>(stat))
                .wrapBy(new FakeCrawler())
                .wrapBy(new FiltrateCrawler())
                .wrapBy(new RateLimitCrawler(0.95f, (iRequest, c) -> System.out.println("超时弹出")))
                ;
        crawler.startup().thenAcceptAsync(c -> {
            System.out.println("爬虫已启动");
            c.push("https://www.baidu.com");
//            c.push("http://localhost:8080/header");
        });
//        Thread.currentThread().join();
        /*while (crawler.isBusy()) {
            Thread.onSpinWait();
        }*/
    }
}
