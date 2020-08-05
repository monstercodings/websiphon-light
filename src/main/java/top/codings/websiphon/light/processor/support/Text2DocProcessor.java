package top.codings.websiphon.light.processor.support;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.processor.AbstractProcessor;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

@Slf4j
@NoArgsConstructor
public class Text2DocProcessor extends AbstractProcessor<String> {
    @Override
    protected Object process0(String data, BuiltinRequest request, ICrawler crawler) throws Exception {
        int code = request.getHttpResponse().statusCode();
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
