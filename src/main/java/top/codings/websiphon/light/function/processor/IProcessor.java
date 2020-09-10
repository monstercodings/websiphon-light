package top.codings.websiphon.light.function.processor;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

public interface IProcessor<T extends IRequest> {
    void process(Object o, T request, ICrawler crawler);
}
