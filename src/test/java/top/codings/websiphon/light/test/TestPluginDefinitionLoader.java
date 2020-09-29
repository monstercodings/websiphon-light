//package top.codings.websiphon.light.test;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import top.codings.websiphon.light.loader.PluginDefinitionLoader;
//
//@Slf4j
//public class TestPluginDefinitionLoader {
//    @Test
//    public void test() {
//        PluginDefinitionLoader loader = new PluginDefinitionLoader("config");
//        assert loader.existJar(
//                "cn.szkedun.spider.mix.plugin",
//                "0.0.1"
//        );
//        assert loader.existClass(
//                "cn.szkedun.spider.mix.plugin.processor.Article2KafkaProcessor",
//                "0.0.1"
//        );
//        loader.findJarByClass(
//                "cn.szkedun.spider.mix.plugin.processor.Article2KafkaProcessor",
//                "0.0.1"
//        ).ifPresentOrElse(jarDefinition -> {
//            System.out.println("找到了 -> " + jarDefinition.getName());
//        }, () -> System.err.println("未找到"));
//    }
//}
