package top.codings.websiphon.light.function.handler;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 具备异步处理能力的响应管理器
 */
@Slf4j
public abstract class AsyncResponseHandler<T extends IRequest>
        extends AbstractResponseHandler<T> implements QueueResponseHandler<T, ICrawler> {
    private static final String NAME = "processors";
    private ExecutorService exe;
    private LinkedTransferQueue<T> queue;
    private Semaphore token;
    /**
     * 该锁用于防止并发响应过程中同时调用完成感知接口的情况
     */
    private Lock lock = new ReentrantLock();
    /**
     * 版本号用于保证每次完成任务时，请求队列监视器和响应管理器只能有其一允许调用完成感知接口
     */
    private AtomicInteger versionConcurrency = new AtomicInteger(0);
    private volatile int verison;
    /**
     * 用于防止非原子操作造成的任务完成情况误判
     */
    private volatile boolean normal = true;

    @Override
    protected void init(ICrawler crawler, int index) throws Exception {
        if (index > 0) {
            return;
        }
        queue = new LinkedTransferQueue<>();
        exe = Executors.newCachedThreadPool(new DefaultThreadFactory(NAME));
        token = new Semaphore(config.getMaxConcurrentProcessing() - 1);
        exe.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 先阻塞获取任务
                    T request = queue.poll(30, TimeUnit.SECONDS);
                    if (null == request) {
                        if (log.isDebugEnabled()) {
                            log.debug("当前剩余响应 -> {}", queue.size());
                        }
                        continue;
                    }
                    // 获取令牌
                    token.acquire();
                    // 将标记位恢复
                    normal = true;
                    exe.submit(() -> {
                        long start = System.currentTimeMillis();
                        try {
                            request.setStatus(IRequest.Status.PROCESS);
                            beforeHandle(request, crawler);
                            handle(request, crawler);
                            afterHandle(request, crawler);
                        } catch (Exception e) {
                            log.error("响应处理发生异常", e);
                        } finally {
                            String useTime = String.format("%.3f", (System.currentTimeMillis() - start) / 1000f);
                            request.setStatus(IRequest.Status.FINISH);
                            token.release();
                            request.release();
                            if (log.isTraceEnabled()) {
                                log.trace("当次处理耗时 [{}s] | 令牌余量 [{}]", useTime, token.availablePermits());
                            }
                            // 检查爬虫是否已空闲
                            if (crawler != null && !crawler.isBusy()) {
                                ICrawler c;
                                if (!CombineCrawler.class.isAssignableFrom(crawler.getClass())) {
                                    c = crawler;
                                } else {
                                    CombineCrawler n = (CombineCrawler) crawler;
                                    c = n.wrapper();
                                }
                                verifyBusy(c, verison);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    token.release();
                    log.error("从响应队列获取任务失败", e);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("消费请求响应线程停止运行");
            }
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
        if (null != queue) queue.clear();
    }

    @Override
    public void handle(T request) {
        normal = false;
        queue.offer(request);
    }

    @Override
    public boolean isBusy() {
        return !(normal &&
                token.availablePermits() == config.getMaxConcurrentProcessing() - 1 &&
                queue.isEmpty()
        );
    }

    private void verifyBusy(ICrawler crawler, final int rawVersion) {
        if (log.isTraceEnabled()) {
            log.trace("响应处理完成，检查任务是否全部完成");
        }
        synchronized (this) {
            // 检查对象内并发的版本号是否与调用前一致
            if (rawVersion != verison) {
                if (log.isTraceEnabled()) {
                    log.trace("另一处理线程已更新版本，忽略本次更新");
                }
                return;
            }
            int newVersion = compareAndIncrementVersion(verison);
            if (verison == newVersion) {
                if (log.isTraceEnabled()) {
                    log.trace("版本更新成功，回掉感知接口");
                }
                verison++;
                finish(crawler);
            } else {
                verison = newVersion;
                if (log.isTraceEnabled()) {
                    log.trace("版本更新失败，最新版本为 -> {}", verison);
                }
            }
        }
    }

    /**
     * 比较并将版本号加一
     *
     * @param nowVersion 成功时返回的值等于入参，失败时返回的值是最新版本号
     * @return
     */
    public int compareAndIncrementVersion(int nowVersion) {
        if (versionConcurrency.compareAndSet(nowVersion, nowVersion + 1)) {
            return nowVersion;
        }
        return versionConcurrency.get();
    }

    protected void beforeHandle(T request, ICrawler crawler) throws Exception {

    }

    protected void afterHandle(T request, ICrawler crawler) throws Exception {

    }

    protected abstract void handle(T request, ICrawler crawler) throws Exception;
}