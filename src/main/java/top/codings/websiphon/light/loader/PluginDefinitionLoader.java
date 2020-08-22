package top.codings.websiphon.light.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.ClassDefinition;
import top.codings.websiphon.light.loader.bean.JarDefinition;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Slf4j
public class PluginDefinitionLoader {
    private final static Object NULL = new Object();
    private Map<JarDefinition, Object> definitions = new ConcurrentHashMap<>();

    public PluginDefinitionLoader(String basePath) {
        FileUtils.listFiles(new File(basePath), new String[]{"jar"}, true)
                .parallelStream()
                .forEach(file -> {
                    try {
                        WebsiphonClassLoader classLoader = new WebsiphonClassLoader(
                                new URL[]{
                                        new URL("file://" + file.getAbsolutePath())
                                }
                        );
                        String p;
                        try (JarFile jarFile = new JarFile(file)) {
                            Manifest manifest = jarFile.getManifest();
                            p = manifest.getMainAttributes().getValue("package");
                        }
                        if (StringUtils.isBlank(p)) {
                            return;
                        }
                        JarDefinition jarDefinition = new JarDefinition();
                        definitions.put(jarDefinition, NULL);
                        List<ClassDefinition> classDefinitions = new LinkedList<>();
                        for (Class<?> clazz : classLoader.findClassByConditionality(new String[]{p}, PluginDefinition.class)) {
                            PluginDefinition pluginDefinition = clazz.getAnnotation(PluginDefinition.class);
                            if (pluginDefinition.primary()) {
                                jarDefinition.setName(pluginDefinition.name());
                                jarDefinition.setDescription(pluginDefinition.description());
                                jarDefinition.setVersion(pluginDefinition.version());
                                jarDefinition.setFullPath(file.getAbsolutePath());
                            } else {
                                ClassDefinition classDefinition = new ClassDefinition();
                                classDefinition.setName(pluginDefinition.name());
                                classDefinition.setDescription(pluginDefinition.description());
                                classDefinition.setClassName(clazz.getName());
                                classDefinition.setJarDefinition(jarDefinition);
                                classDefinitions.add(classDefinition);
                            }
                        }
                        jarDefinition.setClassDefinitions(classDefinitions.toArray(new ClassDefinition[0]));
                    } catch (Exception e) {
                        log.error("扫描Jar包失败", e);
                    }
                });
    }

    public Collection<JarDefinition> getDefinitions() {
        return definitions.keySet();
    }
}
