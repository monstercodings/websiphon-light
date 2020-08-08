package top.codings.websiphon.light.requester.support;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.SyncRequester;

import java.net.http.HttpRequest;
import java.util.concurrent.*;

@Slf4j
public class RateLimitRequester extends CombineRequester<IRequest> implements AsyncRequester<IRequest>, SyncRequester<IRequest> {
    private Semaphore token;
    private int maxNetworkConcurrency;
    private LinkedTransferQueue<IRequest> queue;
    private DelayQueue<Inner> timeoutQueue;
    private ExecutorService exe;
    @Setter
    private CombineCrawler crawler;
    /**
     * 用于防止非原子操作造成的任务完成情况误判
     */
    private volatile boolean normal;

    public RateLimitRequester(CombineRequester requester, int maxNetworkConcurrency) {
        super(requester);
        this.maxNetworkConcurrency = maxNetworkConcurrency;
        token = new Semaphore(maxNetworkConcurrency);
    }

    @Override
    public void init() {
        queue = new LinkedTransferQueue<>();
        timeoutQueue = new DelayQueue<>();
        exe = Executors.newFixedThreadPool(2);
        exe.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    IRequest request;
                    // 先阻塞获取任务
                    request = queue.take();
                    // 获取令牌
                    token.acquire();
                    // 将标记位恢复
                    normal = true;
                    Inner inner = new Inner(request);
                    timeoutQueue.offer(inner);
                    requester.executeAsync(request)
                            .whenCompleteAsync((aVoid, throwable) -> {
                                if (timeoutQueue.remove(inner)) token.release();
                                verifyBusy();
                            })
                    ;
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    token.release();
                    log.error("获取待处理请求对象失败", e);
                }
            }
        });
        exe.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Inner inner = timeoutQueue.take();
                    String url = "";
                    if (inner.request.getHttpRequest() instanceof HttpRequest) {
                        HttpRequest httpRequest = (HttpRequest) inner.request.getHttpRequest();
                        url = httpRequest.uri().toString();
                    }
                    log.warn("请求对象超时 -> {} | {}", inner.status.text, url);
                    token.release();
                    inner.request.release();
                    verifyBusy();
                    /*switch (inner.status) {
                        case REQ -> {
                            log.warn("请求对象超时 -> {} | {}", inner.status.text, inner.request.httpRequest.uri());
                            token.release();
                            if (timeoutQueue.isEmpty() && !crawler.isBusy()) {
                                if (responseHandler instanceof QueueResponseHandler) {
                                    ((QueueResponseHandler) responseHandler).whenFinish(crawler.target());
                                }
                            }
                        }
                        default -> {
                            log.warn("其他状态 -> {} | {}", inner.status.text, inner.request.httpRequest.uri());
                        }
                    }*/
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    log.error("请求对象超时检查失败", e);
                }
            }
        });
        super.init();
    }

    private void verifyBusy() {
        if (queue.isEmpty() && timeoutQueue.isEmpty() && !crawler.wrapper().isBusy()) {
            log.warn("触发强制结束任务");
            IResponseHandler responseHandler = getResponseHandler();
            if (responseHandler instanceof QueueResponseHandler) {
                ((QueueResponseHandler) responseHandler).whenFinish(crawler.wrapper());
            }
        }
    }

    @Override
    public CompletableFuture<IRequest> executeAsync(IRequest request) {
        normal = false;
        queue.offer(request);
        return CompletableFuture.completedFuture(request);
    }

    @Override
    public void shutdown(boolean force) {
        if (null != exe) {
            if (force) exe.shutdownNow();
            else exe.shutdown();
        }
        if (null != queue) {
            queue.clear();
        }
        requester.shutdown(force);
    }

    @Override
    public void setResponseHandler(IResponseHandler responseHandler) {
        ((SyncRequester) requester).setResponseHandler(responseHandler);
    }

    @Override
    public boolean isBusy() {
//        log.debug("正常状态:{} | 剩余令牌:{} | 队列为空:{}", normal, token.availablePermits(), queue.isEmpty());
        return !(normal &&
                token.availablePermits() == maxNetworkConcurrency &&
                queue.isEmpty())
                ;
    }

    @Override
    public void setResponseHandler(QueueResponseHandler responseHandler) {
        ((AsyncRequester) requester).setResponseHandler(responseHandler);
    }

    private static class Inner implements Delayed {
        int timeout = 120000;
        IRequest request;
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

        public Inner(IRequest request) {
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
