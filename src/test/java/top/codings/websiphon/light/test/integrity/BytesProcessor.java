package top.codings.websiphon.light.test.integrity;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.requester.IRequest;

import java.io.File;

@Shared
@Slf4j
public class BytesProcessor extends AbstractProcessor<byte[]> {
    @Override
    protected Object process0(byte[] data, IRequest request, ICrawler crawler) throws Exception {
        log.debug("字节流下载完成");
        File file = new File("data/test.mp4");
        FileUtils.forceMkdirParent(file);
        FileUtils.writeByteArrayToFile(file, data);
        return null;
    }
}
