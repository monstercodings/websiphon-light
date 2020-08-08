package top.codings.websiphon.light.processor.support;

import com.alibaba.fastjson.JSON;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.processor.AbstractProcessor;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

@Slf4j
@NoArgsConstructor
public class JSONProcessor extends AbstractProcessor<JSON> {
    @Override
    protected Object process0(JSON data, IRequest request, ICrawler crawler) throws Exception {
        log.debug("JSON响应\n{}", JSON.toJSONString(data, true));
        return data;
    }
}
