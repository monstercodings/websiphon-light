package top.codings.websiphon.light.function.processor;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.ApacheRequest;
import top.codings.websiphon.light.requester.support.BuiltinRequest;
import top.codings.websiphon.light.requester.support.NettyRequest;

@Slf4j
@NoArgsConstructor
public class Text2DocProcessor extends AbstractProcessor<String> {
    @Override
    protected Object process0(String data, IRequest request, ICrawler crawler) throws Exception {
        int code = -1;
        if (request instanceof BuiltinRequest) {
            code = ((BuiltinRequest) request).getHttpResponse().statusCode();
        } else if (request instanceof ApacheRequest) {
            code = ((ApacheRequest) request).getHttpResponse().getStatusLine().getStatusCode();
        } else if (request instanceof NettyRequest) {
            code = ((NettyRequest) request).getHttpResponse().getCode();
        }
        if (code < 200 || code >= 300) {
            return null;
        }
        Document document = null;
        try {
            document = Jsoup.parse(data);
        } catch (Exception e) {
            if (StringUtils.isBlank(data)) {
                data = "";
            } else {
                data = data.length() > 60 ? data.substring(0, 60) : data;
            }
            log.error("文档化失败 -> {}", data);
        }
        return document;
    }
}
