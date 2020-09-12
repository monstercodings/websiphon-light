package top.codings.websiphon.light.requester.support;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.ComponentFinishAware;
import top.codings.websiphon.light.function.handler.AsyncResponseHandler;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static top.codings.websiphon.light.requester.IRequest.Status.*;

@Slf4j
public class RateLimitRequester extends CombineRequester<IRequest> {
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
    private volatile int verison;
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
                    request.setStatus(REQUEST);
                    Inner inner = new Inner(request, taskTimeoutMillis);
                    timeoutQueue.offer(inner);
                    requester.execute(request)
                            .whenCompleteAsync((aVoid, throwable) -> {
                                // 移除成功说明尚未超时，需要检查任务是否完成
                                if (timeoutQueue.remove(inner)) {
                                    if (null != token) {
                                        token.release();
                                    }
                                    inner.release();
                                    verifyBusy(verison);
                                }
                            })
                    ;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
                    if (null != token) {
                        // 先进行令牌的释放，帮助后续网络请求能更快的发送
                        token.release();
                    }
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
                    verifyBusy(verison);
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

    private void verifyBusy(final int rawVersion) {
        if (queue.isEmpty() && timeoutQueue.isEmpty() && !crawler.wrapper().isBusy()) {
            IResponseHandler responseHandler = getResponseHandler();
            if (responseHandler instanceof AsyncResponseHandler) {
                synchronized (this) {
                    // 检查对象内并发的版本号是否与调用前一致
                    if (rawVersion != verison) {
                        if (log.isTraceEnabled()) {
                            log.trace("另一线程已更新版本，忽略本次更新");
                        }
                        return;
                    }
                    int newVersion = ((AsyncResponseHandler) responseHandler).compareAndIncrementVersion(verison);
                    if (newVersion == verison) {
                        if (log.isTraceEnabled()) {
                            log.trace("版本更新成功，回掉感知接口");
                        }
                        verison++;
                        ((AsyncResponseHandler) responseHandler).finish(crawler.wrapper());
                    } else {
                        verison = newVersion;
                        if (log.isTraceEnabled()) {
                            log.trace("版本更新失败，最新版本为 -> {}", verison);
                        }
                    }
                }
            } else if (responseHandler instanceof ComponentFinishAware) {
                ((ComponentFinishAware) responseHandler).finish(crawler.wrapper());
            }
        }
    }

    @Override
    public CompletableFuture<IRequest> execute(IRequest request) {
        normal = false;
        request.setStatus(READY);
        return CompletableFuture.supplyAsync(() -> {
            request.setStatus(WAIT);
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
        while ((usePercent = checkMemory0()) > limitMemory) {
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
