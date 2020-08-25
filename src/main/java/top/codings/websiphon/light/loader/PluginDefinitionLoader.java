package top.codings.websiphon.light.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.ClassDefinition;
import top.codings.websiphon.light.loader.bean.JarDefinition;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Slf4j
public class PluginDefinitionLoader {
    private final static Object NULL = new Object();
    private final static String DEFAULT_PACKAGE = "top.codings.websiphon.light";
    private Map<JarDefinition, Object> definitions = new ConcurrentHashMap<>();

    public PluginDefinitionLoader(String basePath, String... otherPaths) {
        scanSelf(otherPaths);
        if (StringUtils.isBlank(basePath)) {
            return;
        }
        FileUtils.listFiles(new File(basePath), new String[]{"jar"}, true)
                .parallelStream()
                .forEach(file -> {
                    try {
                        WebsiphonClassLoader classLoader = new WebsiphonClassLoader(
                                new URL[]{
                                        file.toURI().toURL()
                                }
                        );
                        String name;
                        String version;
                        String description;
                        String author;
                        String homepage;
                        String packaging;
                        try (JarFile jarFile = new JarFile(file)) {
                            Manifest manifest = jarFile.getManifest();
                            Attributes attributes = manifest.getMainAttributes();
                            name = attributes.getValue("naming");
                            version = attributes.getValue("version");
                            description = attributes.getValue("desc");
                            author = attributes.getValue("author");
                            homepage = attributes.getValue("homepage");
                            packaging = attributes.getValue("packaging");
                        }
                        if (StringUtils.isBlank(packaging)) {
                            return;
                        }
                        JarDefinition jarDefinition = new JarDefinition(
                                name, version, description, author, homepage, packaging, file.getAbsolutePath());
                        List<ClassDefinition> classDefinitions = new LinkedList<>();
                        for (Class<?> clazz : classLoader.findClassByConditionality(new String[]{packaging}, PluginDefinition.class)) {
                            PluginDefinition pluginDefinition = clazz.getAnnotation(PluginDefinition.class);
                            ClassDefinition classDefinition = new ClassDefinition(
                                    pluginDefinition.name(),
                                    clazz.getName(),
                                    pluginDefinition.description(),
                                    pluginDefinition.type(),
                                    jarDefinition
                            );
                            classDefinitions.add(classDefinition);
                        }
                        jarDefinition.setClassDefinitions(classDefinitions.toArray(new ClassDefinition[0]));
                        definitions.put(jarDefinition, NULL);
                    } catch (Exception e) {
                        log.error("扫描Jar包失败", e);
                    }
                });
    }

    private void scanSelf(String[] otherPaths) {
        String[] scanPaths;
        if (otherPaths.length > 0) {
            scanPaths = Arrays.copyOf(otherPaths, otherPaths.length + 1);
        } else {
            scanPaths = new String[1];
        }
        scanPaths[scanPaths.length - 1] = DEFAULT_PACKAGE;
        WebsiphonClassLoader classLoader = new WebsiphonClassLoader(new URL[0]);
        JarDefinition jarDefinition = new JarDefinition(
                "内置插件库",
                "0.0.1",
                "官方组件内提供的开箱即用的各类组件库",
                "何好听",
                "https://www.codings.top",
                DEFAULT_PACKAGE,
                null
        );
        jarDefinition.setInner(true);
        List<ClassDefinition> classDefinitions = new LinkedList<>();
        for (Class<?> clazz : classLoader.findClassByConditionality(scanPaths, PluginDefinition.class)) {
            PluginDefinition pluginDefinition = clazz.getAnnotation(PluginDefinition.class);
            ClassDefinition classDefinition = new ClassDefinition(
                    pluginDefinition.name(),
                    clazz.getName(),
                    pluginDefinition.description(),
                    pluginDefinition.type(),
                    jarDefinition
            );
            classDefinitions.add(classDefinition);
        }
        jarDefinition.setClassDefinitions(classDefinitions.toArray(new ClassDefinition[0]));
        definitions.put(jarDefinition, NULL);
    }

    public Collection<JarDefinition> getDefinitions() {
        return definitions.keySet();
    }
}