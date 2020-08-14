package top.codings.websiphon.light.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.utils.CompilerUtil;

import java.lang.reflect.Method;

@Slf4j
public class TestCompiler {
    @SneakyThrows
    @Test
    public void test() {
        long start = System.currentTimeMillis();
        Class clazz = CompilerUtil.loadClass("a.b.c.MyTest7");
        long end = System.currentTimeMillis();
        log.debug("加载耗时 -> {}ms", end - start);
        Method method = clazz.getMethod("call", String.class);
        Object o = clazz.getConstructor().newInstance();
        String s = (String) method.invoke(o, "调用方法");
        System.out.println(s);
    }
}
