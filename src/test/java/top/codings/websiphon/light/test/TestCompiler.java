package top.codings.websiphon.light.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.WebsiphonClassLoaderOld;

import java.io.File;
import java.lang.reflect.Method;

@Slf4j
public class TestCompiler {
    @SneakyThrows
    @Test
    public void test() {
        long start = System.currentTimeMillis();
        /*CompilerUtil.compiler("my.response.handler.MyResponseHandler").then(new Consumer<TaskResult>() {
            @Override
            public void accept(TaskResult taskResult) {
                log.debug("编译结果- > {}", taskResult.isSucceed());
            }
        });*/
        WebsiphonClassLoaderOld loader = new WebsiphonClassLoaderOld("config");
        loader.loadClassFromByte("my.response.handler.MyResponseHandler",
                FileUtils.readFileToByteArray(
                        new File("config/compiler/my/response/handler/MyResponseHandler.class")));
        loader.loadClassFromByte("my.response.handler.MyResponseHandler$1",
                FileUtils.readFileToByteArray(
                        new File("config/compiler/my/response/handler/MyResponseHandler$1.class")));
        Class clazz = loader.loadClass("my.response.handler.MyResponseHandler");
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
        WebsiphonClassLoaderOld loader = new WebsiphonClassLoaderOld("config");
        Class clazz = loader.loadClass("my.response.handler.MyResponseHandler");
        Method method = clazz.getDeclaredMethod("processorChain");
        Object o = clazz.getConstructor().newInstance();
        if (!method.canAccess(o)) {
            method.setAccessible(true);
        }
        System.out.println(method.invoke(o));
    }
}
