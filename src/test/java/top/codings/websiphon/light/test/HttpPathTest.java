package top.codings.websiphon.light.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.utils.HttpPathUtil;

public class HttpPathTest {
    @Test
    public void test() {
        HttpPathUtil.absolutely(
                "../../../../../d?id=udik8",
                "https://www.baidu.com:8888/a/b/c?t=123"
        ).ifPresent(System.out::println);
    }
}
