package top.codings.websiphon.light.test;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.light.loader.PluginDefinitionLoader;

public class TestClassLoader {
    @Test
    public void test1() throws Exception {
        PluginDefinitionLoader loader = new PluginDefinitionLoader("data");
        loader.getDefinitions().parallelStream().forEach(jarDefinition -> {
            System.out.println(JSON.toJSONString(jarDefinition, true));
        });
        /*File file = new File("data/spider-processor-plugins.jar");
        WebsiphonClassLoader classloader = new WebsiphonClassLoader(new URL[]{
                new URL("file://" + file.getAbsolutePath()),
        });
        String p;
        try (JarFile jarFile = new JarFile(file)) {
            Manifest manifest = jarFile.getManifest();
            p = manifest.getMainAttributes().getValue("package");
        }
        for (Class<?> clazz : classloader.findClassByConditionality(new String[]{p}, PluginDefinition.class)) {
            PluginDefinition pluginDefinition = clazz.getAnnotation(PluginDefinition.class);
            System.out.println(clazz.getName());
            System.out.println(pluginDefinition.name());
            System.out.println(pluginDefinition.description());
            if (pluginDefinition.primary()) {
                System.out.println(pluginDefinition.version());
            }
            System.out.println("-------------------------");
        }*/

    }
}
