package top.codings.websiphon.light.test.integrity;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.bean.M3u8;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
@Shared
public class M3u8ReceiveProcessor extends AbstractProcessor<M3u8> {

    @Override
    protected Object process0(M3u8 data, IRequest request, ICrawler crawler) throws Exception {
        log.debug("下载完成 -> {} | 大小[{}]", request.getUserData(), data.getContent().length);
        return null;
    }
}
