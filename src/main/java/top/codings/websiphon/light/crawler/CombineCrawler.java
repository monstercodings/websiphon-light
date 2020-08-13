package top.codings.websiphon.light.crawler;

import lombok.Getter;
import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.CombineRequester;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class CombineCrawler implements ICrawler {
    protected CombineCrawler prev;
    protected CombineCrawler next;
    private CombineRequester requester;
    protected CrawlerConfig config;

    @Override
    public CompletableFuture<ICrawler> startup() {
        if (next != null) return next.startup();
        throw new FrameworkException("非代理爬虫必须实现自身方法");
    }

    @Override
    public CompletableFuture<ICrawler> shutdown() {
        if (next != null) return next.shutdown();
        throw new FrameworkException("非代理爬虫必须实现自身方法");
    }

    @Override
    public void push(IRequest request) {
        if (next != null) {
            next.push(request);
            return;
        }
        throw new FrameworkException("非代理爬虫必须实现自身方法");
    }

    @Override
    public void push(String url) {
        if (next != null) {
            next.push(url);
            return;
        }
        throw new FrameworkException("非代理爬虫必须实现自身方法");
    }

    @Override
    public void push(String url, Object userData) {
        if (next != null) {
            next.push(url, userData);
            return;
        }
        throw new FrameworkException("非代理爬虫必须实现自身方法");
    }

    @Override
    public boolean isBusy() {
        if (next != null) return next.isBusy();
        return false;
    }

    @Override
    public CrawlerConfig config() {
        return config;
    }

    protected final CombineRequester getRequester() {
        CombineCrawler prev = this;
        for (CombineCrawler cc = this; cc != null; cc = cc.next) {
            prev = cc;
        }
        return prev.requester;
    }

    protected final void setRequester(CombineRequester requester) {
        CombineCrawler prev = this;
        for (CombineCrawler cc = this; cc != null; cc = cc.next) {
            prev = cc;
        }
        prev.requester = requester;
    }

    /**
     * 对目标爬虫进行代理
     *
     * @param wrapper 代理爬虫
     * @return 代理爬虫
     */
    public final CombineCrawler wrapBy(CombineCrawler wrapper) {
        this.prev = wrapper;
        wrapper.next = this;
        wrapper.config = this.config;
        wrapper.doProxy();
        return wrapper;
    }

    /**
     * 获取爬虫最外层的代理爬虫
     *
     * @return
     */
    public final CombineCrawler wrapper() {
        return prev == null ? this : prev.wrapper();
    }

    protected void doProxy() {
    }

    public <T> Optional<T> find(Class<T> clazz) {
        CombineCrawler o = this;
        for (; o != null && !clazz.isAssignableFrom(o.getClass()); o = o.next) {
        }
        if (null == o) {
            for (o = this.prev; o != null && !clazz.isAssignableFrom(o.getClass()); o = o.prev) {
            }
        }
        return (Optional<T>) Optional.ofNullable(o);
    }
}
