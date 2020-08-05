package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.function.IFilter;

/**
 * 可去重的爬虫
 */
public interface FilterableCrawler {
    IFilter filter();

    void clear();
}
