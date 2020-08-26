package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.config.CrawlerConfig;

public abstract class AbstractResponseHandler implements IResponseHandler {
    protected CrawlerConfig config;

    @Override
    public void setConfig(CrawlerConfig config) {
        this.config = config;
    }
}
