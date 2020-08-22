package top.codings.websiphon.light.test.dependent;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.FilterableCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.handler.StatResponseHandler;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.function.processor.IProcessor;
import top.codings.websiphon.light.function.processor.JSONProcessor;
import top.codings.websiphon.light.function.processor.Text2DocProcessor;
import top.codings.websiphon.light.requester.IRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class TestResponseHandler extends StatResponseHandler {
    @Override
    protected IProcessor processorChain() {
        return new AbstractProcessor<String>() {
            @Override
            protected void init0(ICrawler crawler) {
                if (log.isDebugEnabled()) {
                    log.debug("初始化");
                }
            }

            @Override
            protected void close0() throws IOException {
                if (log.isDebugEnabled()) {
                    log.debug("关闭处理器");
                }
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (log.isDebugEnabled()) {
                    log.debug("休眠结束");
                }
            }

            @Override
            protected Object process0(String data, IRequest request, ICrawler crawler) throws Exception {
                log.debug("[{}] 响应内容:{}", request.getRequestResult().getCode(), (data.length() > 20 ? data.substring(0, 20) : data));
                return data;
            }
        }
                .next(new Text2DocProcessor())
                .next(new JSONProcessor());
        /*return new Text2DocProcessor()
                .next(new AbstractProcessor<Document>() {
                    @Override
                    protected Object process0(Document data, BuiltinRequest request, ICrawler crawler) throws Exception {
                        ((CombineCrawler) crawler).find(FilterableCrawler.class).ifPresent(filterableCrawler -> {
                            if (((TwoCombineFilter) filterableCrawler.filter()).target().put(request.getHttpRequest().uri().toString())) {
                                System.out.println(String.format("新解析 -> %s", data.title()));
                            } else {
                                System.out.println(String.format("该解析已存在 -> %s", data.title()));
                            }
                        });
                        return null;
                    }
                })
                .next(new AbstractProcessor<JSON>() {
                    @Override
                    protected Object process0(JSON data, BuiltinRequest request, ICrawler crawler) throws Exception {
                        System.out.println(JSON.toJSONString(data, true));
                        return null;
                    }
                });*/
    }

    @Override
    protected void handleError(IRequest request, Throwable throwable, ICrawler crawler) {
        log.error("发生异常 -> {}", throwable.getClass().getName());
    }

    ReentrantLock lock = new ReentrantLock();

    @Override
    public void whenFinish(ICrawler crawler) {
        if (!lock.tryLock()) {
            return;
        }
        try {
            ((CombineCrawler) crawler).find(FilterableCrawler.class).ifPresent(FilterableCrawler::clear);
            log.debug("任务已全部完成");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        crawler.shutdown();
    }
}
