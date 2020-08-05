package top.codings.websiphon.light.requester.support;

import lombok.AllArgsConstructor;
import lombok.Setter;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AccomplishedRequester;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 可进行任务完成通知的请求器
 */
public class CompletiveRequester extends CombineRequester implements AccomplishedRequester {
    private DelayQueue<Inner> timeoutQueue;
    @Setter
    private CombineCrawler crawler;

    protected CompletiveRequester(CombineRequester requester) {
        super(requester);
    }

    @Override
    public void init() {
        timeoutQueue = new DelayQueue<>();
        super.init();
    }

    @Override
    public CompletableFuture<BuiltinRequest> executeAsync(BuiltinRequest request) {
        return super.executeAsync(request);
    }

    @Override
    public void complete() {
        IResponseHandler responseHandler = getResponseHandler();
        if (responseHandler instanceof QueueResponseHandler) {
            QueueResponseHandler queueResponseHandler = (QueueResponseHandler) responseHandler;
            queueResponseHandler.whenFinish(crawler.wrapper());
        }
    }

    private static class Inner implements Delayed {
        int timeout = 120000;
        BuiltinRequest request;
        long trigger;
        Status status;

        @AllArgsConstructor
        public enum Status {
            REQ("请求中"),
            RESP("响应中"),
            ERROR("错误"),
            OVER("处理完成");
            String text;
        }

        public Inner(BuiltinRequest request) {
            this.request = request;
            trigger = System.currentTimeMillis() + timeout;
            status = Status.REQ;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(trigger - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            Inner that = (Inner) o;
            if (this.trigger > that.trigger)
                return 1;
            if (this.trigger < that.trigger)
                return -1;
            return 0;
        }
    }
}
