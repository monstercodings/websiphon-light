package top.codings.websiphon.light.function.processor;

import top.codings.websiphon.light.crawler.ICrawler;

public interface ProcessInitAware {
    void init(ICrawler crawler) throws Exception;
}
