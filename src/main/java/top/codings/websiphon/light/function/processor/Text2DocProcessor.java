package top.codings.websiphon.light.function.processor;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;

import java.io.IOException;

@Slf4j
@NoArgsConstructor
@PluginDefinition(name = "文档化处理器", description = "将网页内容文档化", version = "0.0.1", type = PluginType.PROCESSOR)
public class Text2DocProcessor extends AbstractProcessor<String> {
    @Override
    protected Object process0(String data, IRequest request, ICrawler crawler) throws Exception {
        if (!request.getRequestResult().isSucceed()) {
            return null;
        }
        int code = request.getRequestResult().getCode();
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

    @Override
    public void init0(ICrawler crawler) {
    }

    @Override
    protected void close0() throws IOException {
    }
}
