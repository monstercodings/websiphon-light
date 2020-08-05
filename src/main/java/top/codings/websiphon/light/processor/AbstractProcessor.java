package top.codings.websiphon.light.processor;

import io.netty.util.internal.TypeParameterMatcher;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.support.BuiltinRequest;

@Slf4j
public abstract class AbstractProcessor<T> implements IProcessor {
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

    @Override
    public void process(Object o, BuiltinRequest request, ICrawler crawler) {
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

    /*public static void loop(Object o, BuiltinRequest request) {
        for (AbstractProcessor processor : PROCESSORS) {
            if (!processor.fullProcess(o, request)) {
                break;
            }
        }
    }

    public static AbstractProcessor addProcessor(AbstractProcessor... processors) {
        if (processors.length > 0) {
            PROCESSORS.addAll(Arrays.asList(processors));
        }
        if (PROCESSORS.isEmpty()) {
            return null;
        }
        return PROCESSORS.get(0);
    }*/

    protected abstract Object process0(T data, BuiltinRequest request, ICrawler crawler) throws Exception;
}
