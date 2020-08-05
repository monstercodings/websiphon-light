package top.codings.websiphon.light.manager;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.config.RegistryConfig;
import top.codings.websiphon.light.crawler.ICrawler;

public interface IRegistry {
    void startup();

    void shutdown(boolean force);

    void setConfig(CrawlerConfig crawlerConfig, RegistryConfig registryConfig);

    void setCrawler(ICrawler crawler);
}
