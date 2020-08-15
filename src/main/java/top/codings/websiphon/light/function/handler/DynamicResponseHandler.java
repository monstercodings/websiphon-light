package top.codings.websiphon.light.function.handler;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.processor.AbstractProcessor;
import top.codings.websiphon.light.function.processor.IProcessor;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class DynamicResponseHandler extends StatResponseHandler {
    private String[] processorClassNames;
    private ClassLoader classLoader;

    public DynamicResponseHandler(String[] processorClassNames, ClassLoader classLoader) {
        this.processorClassNames = processorClassNames;
        this.classLoader = classLoader;
    }

    @Override
    protected IProcessor processorChain() {
        AbstractProcessor next = null;
        for (String processorClassName : processorClassNames) {
            try {
                AbstractProcessor processor = (AbstractProcessor) classLoader.loadClass(processorClassName).getConstructor().newInstance();
                if (next != null) {
                    next.next(processor);
                }
                next = processor;
            } catch (Exception e) {
                throw new FrameworkException("加载处理链失败", e);
            }
        }
        return next;
    }
}
