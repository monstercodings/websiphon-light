package top.codings.websiphon.light.crawler;

import top.codings.websiphon.light.config.CrawlerConfig;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.support.CombineRequester;

import java.util.Optional;

public abstract class CombineCrawler implements ICrawler {
    protected CombineCrawler prev;
    protected CombineCrawler next;
    private CombineRequester requester;
    protected CrawlerConfig config;

    @Override
    public void startup() {
        if (next != null) next.startup();
    }

    @Override
    public void shutdown() {
        if (next != null) next.shutdown();
    }

    @Override
    public void push(IRequest request) {
        if (next != null) next.push(request);
    }

    @Override
    public void push(String url) {
        if (next != null) next.push(url);
    }

    @Override
    public void push(String url, Object userData) {
        if (next != null) next.push(url, userData);
    }

    @Override
    public boolean isBusy() {
        if (next != null) return next.isBusy();
        return false;
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
     * @param wrapper 被代理的爬虫
     * @return 返回被代理的爬虫
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
