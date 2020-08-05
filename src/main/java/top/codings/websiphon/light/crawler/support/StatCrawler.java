package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.bean.DataStat;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.StatisticalCrawler;
import top.codings.websiphon.light.requester.support.CombineRequester;
import top.codings.websiphon.light.requester.support.StatRequester;

public class StatCrawler<T extends DataStat> extends CombineCrawler implements StatisticalCrawler {
    private T dataStat;

    public StatCrawler(T dataStat) {
        this.dataStat = dataStat;
    }

    @Override
    public T stat() {
        return dataStat;
    }

    @Override
    protected void doProxy() {
        CombineRequester requester = getRequester();
        setRequester(new StatRequester(requester, dataStat));
    }
}
