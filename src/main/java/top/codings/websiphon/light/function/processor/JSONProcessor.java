package top.codings.websiphon.light.function.processor;

import com.alibaba.fastjson.JSON;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

import java.io.IOException;

/**
 * 该处理器为示例程序
 */
@Slf4j
@NoArgsConstructor
public class JSONProcessor extends AbstractProcessor<JSON> {
    @Override
    protected Object process0(JSON data, IRequest request, ICrawler crawler) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("JSON响应\n{}", JSON.toJSONString(data, true));
        }
        return data;
    }

    @Override
    public void init0(ICrawler crawler) {
        if (log.isDebugEnabled()) {
            log.debug("初始化");
        }
    }

    @Override
    protected void close0() throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("关闭处理器");
        }
    }
}
