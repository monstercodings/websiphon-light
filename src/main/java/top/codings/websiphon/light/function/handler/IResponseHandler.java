package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

public interface IResponseHandler {
    void handle(IRequest request);

    void setConfig(CrawlerConfig config);
}
