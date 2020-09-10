package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.requester.IRequest;

public interface IResponseHandler<T extends IRequest> {
    void handle(T request);

    void setConfig(CrawlerConfig config);
}
