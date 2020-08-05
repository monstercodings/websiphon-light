package top.codings.websiphon.light.manager;

import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.StatisticalCrawler;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

/**
 * 具备统计功能的处理器
 * 需要配合具备统计功能的爬虫和请求器来使用
 * 否则单独使用不生效
 */
public abstract class StatResponseHandler extends SimpleResponseHandler {

    /*@Override
    protected AbstractProcessor processorChain() {
        return new AbstractProcessor<>() {
            @Override
            protected Object process0(Object data, BuiltinRequest request, ICrawler crawler) throws Exception {
                if (crawler instanceof CombineCrawler) {
                    CombineCrawler combineCrawler = (CombineCrawler) crawler;
                    combineCrawler.find(StatisticalCrawler.class).ifPresent(statisticalCrawler -> {
                        DataStat dataStat = statisticalCrawler.stat();
                        dataStat.getResponseCountTotal().increment();
                    });

                }
                return data;
            }
        };
    }*/

    @Override
    protected void beforeHandle(BuiltinRequest request) throws Exception {
        super.beforeHandle(request);
    }

    @Override
    protected void afterHandle(BuiltinRequest request) throws Exception {
        if (crawler instanceof CombineCrawler) {
            CombineCrawler combineCrawler = (CombineCrawler) crawler;
            combineCrawler.find(StatisticalCrawler.class).ifPresent(statisticalCrawler -> statisticalCrawler.stat().getResponseCountTotal().increment());
        }
    }
}
