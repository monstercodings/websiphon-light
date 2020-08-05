package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.bean.DataStat;

/**
 * 具备统计功能的爬虫
 */
public interface StatisticalCrawler<T extends DataStat> {
    T stat();
}
