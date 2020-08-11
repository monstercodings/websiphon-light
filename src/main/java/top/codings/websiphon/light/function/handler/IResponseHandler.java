package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;

public interface IResponseHandler {
    void startup(ICrawler crawler);

    void shutdown(boolean force);

    void setConfig(CrawlerConfig config);

    boolean isBusy();
}
