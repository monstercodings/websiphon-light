package top.codings.websiphon.light.function.processor.support;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
@Shared
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
    protected void init(ICrawler crawler, int index) throws Exception {
        if (index > 0) {
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("{}初始化", this.getClass().getSimpleName());
        }
    }

    @Override
    protected void close(int index) throws Exception {
        if (index != 0) {
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("{}被关闭", this.getClass().getSimpleName());
        }
    }
}
