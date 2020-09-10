package top.codings.websiphon.light.function.processor;

import io.netty.util.internal.TypeParameterMatcher;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.error.StopHandlErrorException;
import top.codings.websiphon.light.function.ComponentCloseAware;
import top.codings.websiphon.light.function.ComponentErrorAware;
import top.codings.websiphon.light.function.ComponentInitAware;
import top.codings.websiphon.light.loader.anno.Shared;
import top.codings.websiphon.light.requester.IRequest;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractProcessor<T> implements IProcessor<IRequest>, ComponentInitAware<ICrawler>, ComponentCloseAware, ComponentErrorAware {
    private AtomicInteger initIndex = new AtomicInteger(0);
    private TypeParameterMatcher matcher;
    private AbstractProcessor root;
    private AbstractProcessor prev;
    private AbstractProcessor next;

    public AbstractProcessor() {
        matcher = TypeParameterMatcher.find(this, AbstractProcessor.class, "T");
        root = this;
    }

    public final AbstractProcessor next(AbstractProcessor processor) {
        this.next = processor;
        processor.prev = this;
        processor.root = this.root;
        return processor;
    }

    @Override
    public final void init(ICrawler crawler) throws Exception {
        AbstractProcessor p;
        for (p = root; p != null; p = p.next) {
            if (p.initIndex.compareAndSet(0, 1)) {
                p.init(crawler, 0);
                continue;
            }
            if (p.getClass().getDeclaredAnnotation(Shared.class) == null) {
                throw new FrameworkException(String.format(
                        "[%s]为非共享组件，如需让多个爬虫共享该组件，请使用@Shared注解", p.getClass().getName()
                ));
            }
            p.init(crawler, p.initIndex.getAndIncrement());
        }
    }

    protected void init(ICrawler crawler, int index) throws Exception {
    }

    @Override
    public final void close() throws Exception {
        AbstractProcessor p;
        for (p = root; p != null; p = p.next) {
            p.close(p.initIndex.decrementAndGet());
        }
    }

    protected void close(int index) throws Exception {
    }

    @Override
    public final void process(Object o, IRequest request, ICrawler crawler) {
        for (AbstractProcessor p = root; p != null; p = p.next) {
            if (p.matcher.match(o)) {
                try {
                    o = p.process0(o, request, crawler);
                } catch (Exception e) {
                    log.error("处理数据失败", e);
                    o = null;
                }
            }
            if (null == o) {
                break;
            }
        }
    }

    /**
     * 是否应该使用该处理器对异常进行处理
     *
     * @param request
     * @param throwable
     * @return
     */
    protected boolean isMatchHandleError(IRequest request, Throwable throwable) {
        return false;
    }

    protected void whenError(Throwable throwable, IRequest request, ICrawler crawler) throws Exception {

    }

    @Override
    public final void doOnError(Throwable throwable, IRequest request, ICrawler crawler) throws Exception {
        for (AbstractProcessor p = root; p != null; p = p.next) {
            if (p.isMatchHandleError(request, throwable)) {
                p.whenError(throwable, request, crawler);
            }
        }
    }

    protected void stopHandleError() throws StopHandlErrorException {
        throw new StopHandlErrorException();
    }

    protected abstract Object process0(T data, IRequest request, ICrawler crawler) throws Exception;
}
