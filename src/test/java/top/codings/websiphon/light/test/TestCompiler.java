package top.codings.websiphon.light.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.WebsiphonClassLoader;
import top.codings.websiphon.light.utils.CompilerUtil;

import java.lang.reflect.Method;

@Slf4j
public class TestCompiler {
    @SneakyThrows
    @Test
    public void test() {
        long start = System.currentTimeMillis();
        Class clazz = CompilerUtil.loadClass("my.response.handler.MyResponseHandler");
        long end = System.currentTimeMillis();
        log.debug("加载耗时 -> {}ms", end - start);
        Method method = clazz.getDeclaredMethod("processorChain");
        Object o = clazz.getConstructor().newInstance();
        if (!method.canAccess(o)) {
            method.setAccessible(true);
        }
        System.out.println(method.invoke(o));
    }

    @Test
    public void test2() throws Exception {
        WebsiphonClassLoader loader = new WebsiphonClassLoader();
        Class clazz = loader.loadClass("my.response.handler.MyResponseHandler");
        Method method = clazz.getDeclaredMethod("processorChain");
        Object o = clazz.getConstructor().newInstance();
        if (!method.canAccess(o)) {
            method.setAccessible(true);
        }
        System.out.println(method.invoke(o));
    }
}
