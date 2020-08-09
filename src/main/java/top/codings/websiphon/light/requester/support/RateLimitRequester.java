package top.codings.websiphon.light.requester.support;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.manager.IResponseHandler;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.SyncRequester;

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
        if (maxNetworkConcurrency > 0) {
            token = new Semaphore(maxNetworkConcurrency);
        }
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
                    if (null != token) {
                        // 获取令牌
                        token.acquire();
                    }
                    // 将标记位恢复
                    normal = true;
                    request.setStatus(IRequest.Status.REQUEST);
                    Inner inner = new Inner(request);
                    timeoutQueue.offer(inner);
                    requester.executeAsync(request)
                            .whenCompleteAsync((aVoid, throwable) -> {
                                if (timeoutQueue.remove(inner)) {
                                    if (null != token) {
                                        token.release();
                                    }
                                    inner.release();
                                }
                                verifyBusy();
                            })
                    ;
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    if (null != token) {
                        token.release();
                    }
                    log.error("获取待处理请求对象失败", e);
                }
            }
        });
        exe.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Inner inner = timeoutQueue.take();
                    /*String url = "";
                    if (inner.request instanceof BuiltinRequest) {
                        url = ((HttpRequest) inner.request.getHttpRequest()).uri().toString();
                    } else if (inner.request instanceof ApacheRequest) {
                        url = ((ApacheRequest) inner.request).getHttpRequest().getURI().toString();
                    }*/
                    if (null != token) {
                        // 先进行令牌的释放，帮助后续网络请求能更快的发送
                        token.release();
                    }
                    inner.request.lock();
                    try {
                        IRequest.Status status = inner.request.getStatus();
                        switch (status) {
                            case WAIT, READY, REQUEST -> {
                                inner.request.setStatus(IRequest.Status.TIMEOUT);
//                                inner.request.release();
                                log.warn("请求对象超时 -> {}", inner.request.getStatus().text);
                            }
                        }
                    } finally {
                        inner.request.unlock();
                    }
                    verifyBusy();
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
//            log.warn("请求器执行结束任务操作");
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
        boolean tokenStatu = token == null ? true : token.availablePermits() == maxNetworkConcurrency;
        return !(normal &&
                tokenStatu &&
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

        public void release() {
            request = null;
        }

        public Inner(IRequest request) {
            this.request = request;
            trigger = System.currentTimeMillis() + timeout;
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
