package top.codings.websiphon.light.requester.support;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;

import java.util.concurrent.*;
import java.util.function.BiConsumer;

@Slf4j
public class RateLimitRequester extends CombineRequester<IRequest> {
    private final static String NAME = "并发限制请求器";
    /**
     * 限制内存占用的阈值
     * 设置<=0的话则不做限制
     */
    @Setter
    private float limitMemory;
    private Semaphore token;
    private int maxNetworkConcurrency;
    private LinkedTransferQueue<IRequest> queue;
    private int taskTimeoutMillis;
    private DelayQueue<Inner> timeoutQueue;
    private ExecutorService exe;
    private BiConsumer<IRequest, ICrawler> timeoutHandler;
    @Setter
    private CombineCrawler crawler;
    /**
     * 用于防止非原子操作造成的任务完成情况误判
     */
    private volatile boolean normal;

    public RateLimitRequester(CombineRequester requester, int maxNetworkConcurrency, int taskTimeoutMillis) {
        this(requester, maxNetworkConcurrency, taskTimeoutMillis, null);
    }

    public RateLimitRequester(CombineRequester requester, int maxNetworkConcurrency, int taskTimeoutMillis, BiConsumer<IRequest, ICrawler> timeoutHandler) {
        super(requester);
        this.maxNetworkConcurrency = maxNetworkConcurrency;
        this.taskTimeoutMillis = taskTimeoutMillis;
        if (maxNetworkConcurrency > 0) {
            token = new Semaphore(maxNetworkConcurrency);
        }
        this.timeoutHandler = timeoutHandler;
    }

    @Override
    public CompletableFuture<IRequester> init() {
        CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
            queue = new LinkedTransferQueue<>();
            timeoutQueue = new DelayQueue<>();
            exe = Executors.newFixedThreadPool(2, new DefaultThreadFactory(NAME));
            exe.submit(() -> {
                // TODO 未来做智能阈值
                while (!Thread.currentThread().isInterrupted() && !shutdown) {
                    try {
                        if (limitMemory > 0) {
                            checkMemory();
                        }
                        IRequest request;
                        // 先阻塞获取任务
                        request = queue.poll(30, TimeUnit.SECONDS);
                        if (null == request) {
                            continue;
                        }
                        if (null != token) {
                            // 获取令牌
                            token.acquire();
                        }
                        // 将标记位恢复
                        normal = true;
                        request.setStatus(IRequest.Status.REQUEST);
                        Inner inner = new Inner(request, taskTimeoutMillis);
                        timeoutQueue.offer(inner);
                        requester.execute(request)
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
                        break;
                    } catch (Exception e) {
                        if (null != token) {
                            token.release();
                        }
                        log.error("获取待处理请求对象失败", e);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("消费请求任务线程停止运行");
                }
            });
            exe.submit(() -> {
                while (!Thread.currentThread().isInterrupted() && !shutdown) {
                    try {
                        Inner inner = timeoutQueue.poll(30, TimeUnit.SECONDS);
                        if (inner == null) {
                            continue;
                        }
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
                                    if (null != timeoutHandler) {
                                        try {
                                            timeoutHandler.accept(inner.request, crawler.wrapper());
                                        } catch (Exception e) {
                                            log.error("请求对象超时处理失败", e);
                                        }
                                    }
//                                log.warn("请求对象超时 -> {}", inner.request.getStatus().text);
                                    inner.request.setStatus(IRequest.Status.TIMEOUT);
//                                inner.request.release();
                                }
                            }
                        } finally {
                            inner.request.unlock();
                        }
                        verifyBusy();
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        log.error("请求对象超时检查失败", e);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("监测请求任务超时线程停止运行");
                }
            });
            return this;
        });
        return completableFuture.thenCombineAsync(super.init(), (o, o2) -> o2);
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
    public CompletableFuture<IRequest> execute(IRequest request) {
        normal = false;
        request.setStatus(IRequest.Status.READY);
        return CompletableFuture.supplyAsync(() -> {
            request.setStatus(IRequest.Status.WAIT);
            queue.offer(request);
            return request;
        });
    }

    @Override
    public CompletableFuture<IRequester> shutdown(boolean force) {
        if (null != exe) {
            if (force) exe.shutdownNow();
            else exe.shutdown();
            try {
                exe.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
            }
        }
        if (null != queue) {
            queue.clear();
        }
        shutdown = true;
        return super.shutdown(force);
    }

    @Override
    public boolean isBusy() {
        if (log.isTraceEnabled()) {
            log.trace("正常状态:{} | 剩余令牌:{} | 队列为空:{}", normal, token.availablePermits(), queue.isEmpty());
        }
        boolean tokenStatu = token == null ? true : token.availablePermits() == maxNetworkConcurrency;
        return !(normal &&
                tokenStatu &&
                queue.isEmpty())
                ;
    }

    private void checkMemory() throws InterruptedException {
        boolean first = true;
        float usePercent;
        long startTime = 0;
        while ((usePercent = _checkMemory()) > limitMemory) {
            if (first) {
                first = false;
                startTime = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug("内存使用百分比超出设定阈值{}%，当前使用{}%", String.format("%.2f", limitMemory * 100f), String.format("%.2f", usePercent * 100f));
                }
            }
            Thread.onSpinWait();
            if (log.isTraceEnabled()) {
                log.trace("休眠结束，再次检查内存占用情况 | {}%", String.format("%.2f", usePercent * 100f));
            }
            if (System.currentTimeMillis() - startTime > 60 * 1000l) {
                first = true;
            }
        }
    }

    private float _checkMemory() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        //jvm总内存
        long jvmTotalMemoryByte = runtime.totalMemory();
        //jvm最大可申请
        long jvmMaxMoryByte = runtime.maxMemory();
        // 空闲空间
        long freeMemoryByte = runtime.freeMemory();
        // 已使用
        long usedMemory = jvmTotalMemoryByte - freeMemoryByte;
        // 已使用JVM内存百分比
        float usePercent = usedMemory * 1f / jvmMaxMoryByte;
        return usePercent;
    }

    private static class Inner implements Delayed {
        IRequest request;
        long trigger;

        public void release() {
            request = null;
        }

        public Inner(IRequest request, int timeout) {
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
