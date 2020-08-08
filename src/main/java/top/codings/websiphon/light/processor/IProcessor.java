package top.codings.websiphon.light.processor;

import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

public interface IProcessor {
    void process(Object o, IRequest request, ICrawler crawler);
}
