package top.codings.websiphon.light.function.processor.support;

import io.netty.util.internal.TypeParameterMatcher;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.function.processor.IProcessor;
import top.codings.websiphon.light.function.processor.ProcessCloseAware;
import top.codings.websiphon.light.function.processor.ProcessInitAware;
import top.codings.websiphon.light.requester.IRequest;

@Slf4j
public abstract class AbstractProcessor<T> implements IProcessor, ProcessInitAware, ProcessCloseAware {
    private TypeParameterMatcher matcher;
    private AbstractProcessor root;
    private AbstractProcessor prev;
    private AbstractProcessor next;

    public AbstractProcessor() {
        matcher = TypeParameterMatcher.find(this, AbstractProcessor.class, "T");
        root = this;
    }

    public AbstractProcessor next(AbstractProcessor processor) {
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
    public void process(Object o, IRequest request, ICrawler crawler) {
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

    protected abstract Object process0(T data, IRequest request, ICrawler crawler) throws Exception;
}
