package top.codings.websiphon.light.function.handler;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 处理器链的响应处理器
 */
@Slf4j
public abstract class ChainResponseHandler implements QueueResponseHandler {
    private final static String NAME = "处理链";
    protected CrawlerConfig config;
    private ExecutorService exe;
    private LinkedTransferQueue<IRequest> queue;
    private Semaphore token;
    private Lock lock = new ReentrantLock();
//    private ICrawler crawler;
    /**
     * 用于防止非原子操作造成的任务完成情况误判
     */
    private volatile boolean normal = true;

    @Override
    public CompletableFuture<IResponseHandler> startup(ICrawler crawler) {
        return CompletableFuture.supplyAsync(()->{
            queue = new LinkedTransferQueue<>();
            exe = Executors.newCachedThreadPool(new DefaultThreadFactory(NAME));
            token = new Semaphore(config.getMaxConcurrentProcessing() - 1);
            exe.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // 先阻塞获取任务
                        IRequest request = queue.poll(1, TimeUnit.MINUTES);
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
                                if (crawler != null && !crawler.isBusy() && lock.tryLock()) {
                                    try {
                                        if (!crawler.isBusy()) {
                                            ICrawler c;
                                            if (!CombineCrawler.class.isAssignableFrom(crawler.getClass())) {
                                                c = crawler;
                                            } else {
                                                CombineCrawler n = (CombineCrawler) crawler;
                                                c = n.wrapper();
                                            }
                                            whenFinish(c);
                                        }
                                    } finally {
                                        lock.unlock();
                                    }
                                }
                            }
                        });
                    } catch (InterruptedException e) {
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
            return this;
        });
    }

    @Override
    public CompletableFuture<IResponseHandler> shutdown(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            if (null != exe) {
                if (force) exe.shutdownNow();
                else exe.shutdown();
                try {
                    exe.awaitTermination(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                }
            }
            if (null != queue) queue.clear();
            return this;
        });
    }

    @Override
    public void handle(IRequest request) {
        normal = false;
        queue.offer(request);
    }

    @Override
    public void setConfig(CrawlerConfig config) {
        this.config = config;
    }

    @Override
    public boolean isBusy() {
        return !(normal &&
                token.availablePermits() == config.getMaxConcurrentProcessing() - 1 &&
                queue.isEmpty()
        );
    }

    /*@Override
    public void setCrawler(ICrawler crawler) {
        this.crawler = crawler;
    }*/

    protected void beforeHandle(IRequest request, ICrawler crawler) throws Exception {

    }

    protected void afterHandle(IRequest request, ICrawler crawler) throws Exception {

    }

    protected abstract void handle(IRequest request, ICrawler crawler) throws Exception;
}
