package top.codings.websiphon.light.function.processor;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

public interface IProcessor {
    void process(Object o, IRequest request, ICrawler crawler);
}
