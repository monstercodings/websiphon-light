package top.codings.websiphon.light.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.bean.ClassAndJarCache;

public class TestClassAndJarCache {
    @Test
    public void test() {
        ClassAndJarCache cache = new ClassAndJarCache("config");
        cache.loadClassByte("my.response.handler.MyResponseHandler$1", "0.0.1");
        cache.loadClassByte("top.codings.vdash.protocol.component.protocol.packet.v1.TransferPacket$Response", "0.0.11");
    }
}
