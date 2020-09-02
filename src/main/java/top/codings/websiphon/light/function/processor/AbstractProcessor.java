package top.codings.websiphon.light.function.processor;

import io.netty.util.internal.TypeParameterMatcher;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.error.StopHandlErrorException;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
public abstract class AbstractProcessor<T> implements IProcessor, ProcessInitAware, ProcessCloseAware, ProcessErrorAware {
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

    public final void initByHandler(ICrawler crawler) throws Exception {
        AbstractProcessor p;
        for (p = root; p != null; p = p.next) {
            p.init(crawler);
        }
    }

    @Override
    public void init(ICrawler crawler) throws Exception {
    }

    public final void closeByHandler() throws Exception {
        AbstractProcessor p;
        for (p = root; p != null; p = p.next) {
            p.close();
        }
    }

    @Override
    public void close() throws Exception {
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

    protected boolean isMatch(IRequest request, Throwable throwable) {
        return false;
    }

    protected boolean isTransmit(IRequest request, Throwable throwable, ICrawler crawler) {
        return true;
    }

    protected void whenError(IRequest request, Throwable throwable, ICrawler crawler) {

    }

    @Override
    public final void doOnError(IRequest request, Throwable throwable, ICrawler crawler) throws StopHandlErrorException {
        boolean transmit = true;
        for (AbstractProcessor p = root; p != null; p = p.next) {
            if (p.isMatch(request, throwable)) {
                try {
                    p.whenError(request, throwable, crawler);
                    transmit = p.isTransmit(request, throwable, crawler);
                    if (!transmit) {
                        break;
                    }
                } catch (Exception e) {
                    log.error("处理器执行异常处理失败", e);
                    continue;
                }
            }
        }
        if (!transmit) {
            throw new StopHandlErrorException();
        }
    }

    protected abstract Object process0(T data, IRequest request, ICrawler crawler) throws Exception;
}
