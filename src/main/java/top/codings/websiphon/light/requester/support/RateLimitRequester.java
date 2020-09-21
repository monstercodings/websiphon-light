package top.codings.websiphon.light.requester.support;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.ComponentCountAware;
import top.codings.websiphon.light.function.ComponentFinishAware;
import top.codings.websiphon.light.function.handler.AsyncResponseHandler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static top.codings.websiphon.light.requester.IRequest.Status.*;

@Slf4j
public class RateLimitRequester extends CombineRequester<IRequest> implements ComponentCountAware {
    private static final String NAME = "ratelimit";
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
    protected void init(ICrawler iCrawler, int index) throws Exception {
        if (index > 0) {
            return;
        }
        if (iCrawler instanceof CombineCrawler) {
            this.crawler = ((CombineCrawler) iCrawler).wrapper();
        } else {
            throw new FrameworkException("爬虫对象必须是可组合类型");
        }
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
                    request = queue.take();
                    if (null != token) {
                        // 获取令牌
                        token.acquire();
                    }
                    // 将标记位恢复
                    normal = true;
                    request.setStatus(READY);
                    syncBackpress();
                    request.setStatus(REQUEST);
                    Inner inner = new Inner(request, taskTimeoutMillis);
                    timeoutQueue.offer(inner);

                    CompletableFuture completableFuture = super.execute(request);
                    completableFuture.whenCompleteAsync((req, throwable) -> {
                        timeoutQueue.remove(inner);
                        IResponseHandler responseHandler = getResponseHandler();
                        int nowVersion = -1;
                        if (responseHandler instanceof AsyncResponseHandler) {
                            nowVersion = ((AsyncResponseHandler) responseHandler).currentVersion();
                        }
                        if (null != token) {
                            token.release();
                        }
                        verifyBusy(nowVersion);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
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
                    Inner inner = timeoutQueue.take();
                    inner.request.lock();
                    try {
                        IRequest.Status status = inner.request.getStatus();
                        if (status == WAIT || status == READY || status == REQUEST) {
                            if (null != timeoutHandler) {
                                try {
                                    timeoutHandler.accept(inner.request, crawler.wrapper());
                                } catch (Exception e) {
                                    log.error("请求对象超时处理失败", e);
                                }
                            }
                            inner.request.setStatus(IRequest.Status.TIMEOUT);
                            inner.request.stop();
                        }
                    } finally {
                        inner.request.unlock();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("请求对象超时检查失败", e);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("监测请求任务超时线程停止运行");
            }
        });
    }

    private void syncBackpress() throws InterruptedException {
        IResponseHandler responseHandler = getResponseHandler();
        if (responseHandler instanceof AsyncResponseHandler) {
            ((AsyncResponseHandler) responseHandler).syncBackpress();
        }
    }

    private void verifyBusy(final int currentVersion) {
        IResponseHandler responseHandler = getResponseHandler();
        if (responseHandler instanceof AsyncResponseHandler) {
            ((AsyncResponseHandler) responseHandler).verifyBusy(crawler.wrapper(), currentVersion);
        } else if (
                (responseHandler instanceof ComponentFinishAware) &&
                        !crawler.wrapper().isBusy()) {
            ((ComponentFinishAware) responseHandler).finish(crawler.wrapper());
        }
    }

    @Override
    public CompletableFuture<IRequest> execute(IRequest request) {
        normal = false;
        request.setStatus(WAIT);
        return CompletableFuture.supplyAsync(() -> {
            queue.offer(request);
            return request;
        });
    }

    @Override
    protected void close(int index) throws Exception {
        if (index != 0) {
            return;
        }
        if (null != exe) {
            exe.shutdownNow();
            try {
                exe.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (null != queue) {
            queue.clear();
        }
        shutdown = true;
        super.close();
    }

    @Override
    public boolean isBusy() {
        // 若队列不为空则一定有等待执行的任务
        if (!queue.isEmpty()) {
            return true;
        }
        // 如果令牌不为空则检查令牌数量
        if (token != null) {
            int availablePermits = token.availablePermits();
            // 令牌数量尚未恢复最大值则有正在执行的任务
            if (availablePermits < maxNetworkConcurrency) {
                if (log.isTraceEnabled()) {
                    log.trace("当前令牌数[{}] | 预置令牌数[{}]", availablePermits, maxNetworkConcurrency);
                }
                return true;
            }
        }
        // 前两项都通过后，检查响应管理器是否正在提交新任务
        if (!normal) {
            if (log.isTraceEnabled()) {
                log.trace("响应管理器正在提交新任务");
            }
            return true;
        }
        return false;
    }

    private void checkMemory() throws InterruptedException {
        boolean first = true;
        float usePercent;
        long startTime = 0;
        while ((usePercent = checkMemory0()) > limitMemory) {
            if (first) {
                first = false;
                startTime = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug("内存使用百分比超出设定阈值{}%，当前使用{}%", String.format("%.2f", limitMemory * 100f), String.format("%.2f", usePercent * 100f));
                }
            }
            Thread.sleep(2000);
            /*if (log.isTraceEnabled()) {
                log.trace("休眠结束，再次检查内存占用情况 | {}%", String.format("%.2f", usePercent * 100f));
            }*/
            if (System.currentTimeMillis() - startTime > 60 * 1000l) {
                first = true;
            }
        }
    }

    private float checkMemory0() throws InterruptedException {
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

    @Override
    public int count() {
        return queue.size() + timeoutQueue.size();
    }

    private static class Inner implements Delayed {
        IRequest request;
        long trigger;

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
