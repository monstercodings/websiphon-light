package top.codings.websiphon.light.test.dependent;

import lombok.SneakyThrows;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.FilterableCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.manager.StatResponseHandler;
import top.codings.websiphon.light.processor.AbstractProcessor;
import top.codings.websiphon.light.processor.IProcessor;
import top.codings.websiphon.light.processor.support.JSONProcessor;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.locks.ReentrantLock;

public class TestResponseHandler extends StatResponseHandler {
    @Override
    protected IProcessor processorChain() {
        return new AbstractProcessor<String>() {
            @Override
            protected Object process0(String data, IRequest request, ICrawler crawler) throws Exception {
//                System.out.println("响应内容如下:\n" + data);
//                Thread.sleep(200);
                return data;
            }
        }.next(new JSONProcessor());
        /*return new Text2DocProcessor()
                .next(new AbstractProcessor<Document>() {
                    @Override
                    protected Object process0(Document data, BuiltinRequest request, ICrawler crawler) throws Exception {
                        ((CombineCrawler) crawler).find(FilterableCrawler.class).ifPresent(filterableCrawler -> {
                            if (((CombineFilter) filterableCrawler.filter()).target().put(request.getHttpRequest().uri().toString())) {
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
//        System.err.println("发生异常 -> " + throwable.getClass().getName());
    }

    ReentrantLock lock = new ReentrantLock();

    @Override
    public void whenFinish(ICrawler crawler) {
        if (!lock.tryLock()) {
            return;
        }
        ((CombineCrawler) crawler).find(FilterableCrawler.class).ifPresent(FilterableCrawler::clear);
        System.out.println(Thread.currentThread().getName() + ": 任务已全部完成");
        lock.unlock();
//        crawler.shutdown();
    }
}
