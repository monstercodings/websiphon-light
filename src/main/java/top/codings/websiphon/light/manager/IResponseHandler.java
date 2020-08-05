package top.codings.websiphon.light.manager;

import top.codings.websiphon.light.config.CrawlerConfig;

public interface IResponseHandler {
    void startup();

    void shutdown(boolean force);

    void setConfig(CrawlerConfig config);

    boolean isBusy();
}
