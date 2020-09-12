package top.codings.websiphon.light.test.integrity;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.FilterableCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.handler.StatResponseHandler;
import top.codings.websiphon.light.function.processor.IProcessor;
import top.codings.websiphon.light.function.processor.support.M3u8DownloadProcessor;
import top.codings.websiphon.light.function.processor.support.Text2DocProcessor;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
@Shared
public class TestResponseHandler extends StatResponseHandler<IRequest> {
    private static ContentPrintProcessor contentPrintProcessor = new ContentPrintProcessor();
    private static Text2DocProcessor text2DocProcessor = new Text2DocProcessor();
    private static M3u8DownloadProcessor m3u8DownloadProcessor = new M3u8DownloadProcessor(true);
    private static M3u8ReceiveProcessor m3u8ReceiveProcessor = new M3u8ReceiveProcessor();
    private static BytesProcessor bytesProcessor = new BytesProcessor();

    @Override
    protected IProcessor processorChain() {
        return contentPrintProcessor
                .next(text2DocProcessor)
                .next(m3u8DownloadProcessor)
                .next(m3u8ReceiveProcessor)
                .next(bytesProcessor)
                ;
    }

    @Override
    protected void handleError(IRequest request, Throwable throwable, ICrawler crawler) {
        log.error("发生异常", throwable);
    }

    @Override
    public void finish(ICrawler crawler) {
        /*if (!lock.tryLock()) {
            return;
        }*/
        try {
            ((CombineCrawler) crawler).find(FilterableCrawler.class).ifPresent(FilterableCrawler::clear);
            log.debug("任务已全部完成");
        } catch (Exception e) {
            e.printStackTrace();
        }/* finally {
            lock.unlock();
        }*/
//        crawler.shutdown();
    }
}
