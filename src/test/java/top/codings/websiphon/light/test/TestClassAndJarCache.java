package top.codings.websiphon.light.test;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.ClassAndJarCache;
import top.codings.websiphon.light.bean.WebsiphonClassLoader;

public class TestClassAndJarCache {
    @Test
    public void test() {
        ClassAndJarCache cache = new ClassAndJarCache("config");
        System.out.println(JSON.toJSONString(cache.getClass2Path(), true));
        cache.loadClassByte("my.response.handler.MyResponseHandler$1", "0.0.1");
        cache.loadClassByte("top.codings.vdash.protocol.component.protocol.packet.v1.TransferPacket$Response", "0.0.11");
    }

    @Test
    public void test2() {
//        String name = "top.codings.vdash.protocol.component.protocol.packet.v1.TransferPacket$Response";
        String name = "my.response.handler.MyResponseHandler$1";
        String version = "0.0.1";
        WebsiphonClassLoader loader = new WebsiphonClassLoader("config");
        Class<?> clazz;
        try {
            clazz = loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            System.err.println("未找到该类");
            clazz = loader.loadClass(name, version);
        }
        System.out.println("找到该类 -> " + clazz.getName());
    }
}