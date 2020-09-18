package top.codings.websiphon.light.function.handler;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.ComponentCountAware;
import top.codings.websiphon.light.requester.IRequest;

import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 具备异步处理能力的响应管理器
 */
@Slf4j
public abstract class AsyncResponseHandler<T extends IRequest>
        extends AbstractResponseHandler<T> implements QueueResponseHandler<T, ICrawler>, ComponentCountAware {
    private static final String NAME = "processors";
    private ExecutorService exe;
    private LinkedTransferQueue<T> queue;
    private Semaphore token;
    /**
     * 该锁用于防止并发响应过程中同时调用完成感知接口的情况
     */
//    private Lock lock = new ReentrantLock();
    private int maxConcurrentProcessiong;
    /**
     * 版本号用于保证每次完成任务时，请求队列监视器和响应管理器只能有其一允许调用完成感知接口
     */
    private AtomicInteger versionConcurrency = new AtomicInteger(0);
    /**
     * 用于防止非原子操作造成的任务完成情况误判
     */
    private volatile boolean normal = true;

    @Override
    protected void init(final ICrawler crawler, int index) throws Exception {
        if (index > 0) {
            return;
        }
        queue = new LinkedTransferQueue<>();
        exe = Executors.newCachedThreadPool(new DefaultThreadFactory(NAME));
        maxConcurrentProcessiong = config.getMaxConcurrentProcessing();
        token = new Semaphore(maxConcurrentProcessiong);
        exe.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 先阻塞获取任务
                    T request = queue.take();
                    // 获取令牌
                    token.acquire();
                    // 将标记位恢复
                    normal = true;
                    exe.submit(() -> {
                        Throwable cause = null;
                        try {
                            request.setStatus(IRequest.Status.PROCESS);
                            beforeHandle(request, crawler);
                            handle(request, crawler);
                        } catch (Exception e) {
                            cause = e;
                            log.error("响应处理发生异常", e);
                        } finally {
                            try {
                                afterHandle(request, crawler, cause);
                            } catch (Exception e) {
                                log.error("后置处理发生异常", e);
                            }
                            // 必须在令牌释放前缓存当前版本号
                            int nowVersion = currentVersion();
                            request.setStatus(IRequest.Status.FINISH);
                            token.release();
                            request.release();
                            // 检查爬虫是否已空闲
                            verifyBusy(crawler, nowVersion);
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
        // 若队列不为空则一定有等待执行的任务
        if (!queue.isEmpty()) {
            return true;
        }
        // 如果令牌不为空则检查令牌数量
        if (token != null) {
            int availablePermits = token.availablePermits();
            // 令牌数量尚未恢复最大值则有正在执行的任务
            if (availablePermits < maxConcurrentProcessiong) {
                if (log.isTraceEnabled()) {
                    log.trace("当前令牌数[{}] | 预置令牌数[{}]", availablePermits, maxConcurrentProcessiong);
                }
                return true;
            }
        }
        // 前两项都通过后，检查请求器是否正在提交新响应
        if (!normal) {
            if (log.isTraceEnabled()) {
                log.trace("请求器正在提交新响应");
            }
            return true;
        }
        return false;
    }

    /**
     * 检测是否繁忙，如果空闲使用版本号原子操作确保并发情况下只调用一次完成回调感知接口
     *
     * @param crawler
     * @param currentVersion
     */
    public void verifyBusy(final ICrawler crawler, final int currentVersion) {
        if (crawler == null || crawler.isBusy()) {
            return;
        }
        // 检查对象内并发的版本号是否与调用前一致
        if (!compareAndIncrementVersion(currentVersion)) {
            if (log.isDebugEnabled()) {
                log.debug("另一处理线程已更新版本，忽略本次更新");
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("版本更新成功，回调完成任务感知接口");
        }
        ICrawler c = crawler;
        if (crawler instanceof CombineCrawler || CombineCrawler.class.isAssignableFrom(crawler.getClass())) {
            c = ((CombineCrawler) crawler).wrapper();
        }
        finish(c);
    }

    /**
     * 比较并将版本号加一
     *
     * @param nowVersion 成功时返回的值等于入参，失败时返回的值是最新版本号
     * @return
     */
    private boolean compareAndIncrementVersion(int nowVersion) {
        return versionConcurrency.compareAndSet(nowVersion, nowVersion + 1);
    }

    public int currentVersion() {
        return versionConcurrency.get();
    }

    @Override
    public int count() {
        return queue.size();
    }

    protected void beforeHandle(T request, ICrawler crawler) throws Exception {

    }

    protected void afterHandle(T request, ICrawler crawler, @Nullable Throwable cause) {

    }

    protected abstract void handle(T request, ICrawler crawler) throws Exception;
}
