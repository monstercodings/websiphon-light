package top.codings.websiphon.light.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.loader.WebsiphonClassLoader;
import top.codings.websiphon.light.loader.anno.PluginDefinition;

import java.io.File;
import java.net.URL;

public class TestClassLoader {
    @Test
    public void test1() throws Exception {
        String packageName1 = "top.codings.crawler.processor.plugins";
        String packageName2 = "top.codings.websiphon.light";
        WebsiphonClassLoader classloader = new WebsiphonClassLoader(new URL[]{
                new URL("file://" + new File("data/spider-processor-plugins.jar").getAbsolutePath()),
        });
        for (Class<?> clazz : classloader.findClassByConditionality(new String[]{packageName1, packageName2}, PluginDefinition.class)) {
            PluginDefinition pluginDefinition = clazz.getAnnotation(PluginDefinition.class);
            System.out.println(pluginDefinition.name());
            System.out.println(pluginDefinition.version());
            System.out.println(pluginDefinition.description());
            System.out.println(pluginDefinition.primary());
        }

    }
}
