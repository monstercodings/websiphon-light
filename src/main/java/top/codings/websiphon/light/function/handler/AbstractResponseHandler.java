package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.ComponentCloseAware;
import top.codings.websiphon.light.function.ComponentInitAware;
import top.codings.websiphon.light.loader.anno.Shared;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractResponseHandler implements
        IResponseHandler, ComponentInitAware<ICrawler>, ComponentCloseAware {
    private transient AtomicInteger initIndex = new AtomicInteger(0);
    protected CrawlerConfig config;

    @Override
    public void setConfig(CrawlerConfig config) {
        this.config = config;
    }

    @Override
    public final void init(ICrawler crawler) throws Exception {
        if (!initIndex.compareAndSet(0, 1)) {
            if (getClass().getDeclaredAnnotation(Shared.class) == null) {
                throw new FrameworkException(String.format(
                        "[%s]为非共享组件，如需使用单例供多个爬虫使用则需使用 @Shared 注解修饰该组件",
                        getClass().getName()
                ));
            }
            init(crawler, initIndex.getAndIncrement());
            return;
        }
        init(crawler, 0);
    }

    /**
     * 初始化响应管理器
     *
     * @param crawler 所属的爬虫
     * @param index   当前初始化的索引值
     */
    protected void init(ICrawler crawler, int index) throws Exception {
    }

    @Override
    public final void close() throws Exception {
        int count = initIndex.decrementAndGet();
        if (count < 0) return;
        close(count);
    }

    /**
     * 关闭响应管理器
     *
     * @param index 当前关闭的索引值
     */
    protected void close(int index) throws Exception {
    }
}
