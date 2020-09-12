package top.codings.websiphon.light.test.integrity;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
@Shared
public class ContentPrintProcessor extends AbstractProcessor<String> {
    @Override
    protected Object process0(String data, IRequest request, ICrawler crawler) throws Exception {
        String content = data.length() > 2 ? data.substring(0, 2) : data;
//                content = data;
        log.debug("[{}] 响应内容:{}", request.getRequestResult().getCode(), content);
        return data;
    }
}
