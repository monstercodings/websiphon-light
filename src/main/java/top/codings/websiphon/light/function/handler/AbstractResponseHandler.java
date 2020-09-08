package top.codings.websiphon.light.function.handler;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.ComponentInitAware;
import top.codings.websiphon.light.loader.anno.Shared;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractResponseHandler implements IResponseHandler, ComponentInitAware<ICrawler> {
    private transient volatile boolean init;
    protected CrawlerConfig config;

    @Override
    public void setConfig(CrawlerConfig config) {
        this.config = config;
    }

    @Override
    public void init(ICrawler crawler) throws Exception {
        synchronized (this) {
            if (init && getClass().getDeclaredAnnotation(Shared.class) == null) {
                throw new FrameworkException(String.format(
                        "[%s]非共享组件，如需使用单例供多个爬虫使用则需使用@Shared注解修饰该组件",
                        getClass().getName()
                ));
            }
            init = true;
        }
    }
}
