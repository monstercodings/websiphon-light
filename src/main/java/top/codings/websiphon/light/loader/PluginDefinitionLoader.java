package top.codings.websiphon.light.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.ClassDefinition;
import top.codings.websiphon.light.loader.bean.JarDefinition;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Slf4j
public class PluginDefinitionLoader {
    private final static String DEFAULT_PACKAGE = "top.codings.websiphon.light";
    private Map<String, JarDefinition> definitions = new ConcurrentHashMap<>();
    private String basePath;
    private String[] otherPaths;

    public PluginDefinitionLoader(String basePath, String... otherPaths) {
        this.basePath = basePath;
        this.otherPaths = otherPaths;
        reScan();
    }

    public void reScan() {
        definitions.clear();
        scanSelf(otherPaths);
        if (StringUtils.isBlank(basePath)) {
            return;
        }
        FileUtils.listFiles(new File(basePath), new String[]{"jar"}, true)
                .parallelStream()
                .forEach(file -> {
                    try (WebsiphonClassLoader classLoader = new WebsiphonClassLoader(
                            new URL[]{
                                    file.toURI().toURL()
                            }
                    )) {
                        String name;
                        String version;
                        String description;
                        String author;
                        String homepage;
                        String packaging;
                        try (JarFile jarFile = new JarFile(file);
                             BufferedInputStream bis = new BufferedInputStream(
                                     jarFile.getInputStream(jarFile.getEntry("plugin.properties")))
                        ) {
                            Properties properties = new Properties();
                            properties.load(bis);
                            name = properties.getProperty("plugin.name");
                            version = properties.getProperty("plugin.version");
                            description = properties.getProperty("plugin.desc");
                            author = properties.getProperty("plugin.author");
                            homepage = properties.getProperty("plugin.homepage");
                            packaging = properties.getProperty("plugin.package");
                        } catch (Exception e) {
                            log.error("读取Jar包内配置失败 -> {}", file.getAbsolutePath(), e);
                            return;
                        }
                        if (StringUtils.isAnyBlank(name, version, description, author, packaging)) {
                            log.warn("jar包plugin.properties信息不完整 -> {}", file.getAbsolutePath());
                            return;
                        }
                        JarDefinition jarDefinition = new JarDefinition(
                                name, version, description, author, homepage, packaging, file.getAbsolutePath());
                        String id = getJarId(jarDefinition);
                        JarDefinition temp = definitions.putIfAbsent(id, jarDefinition);
                        if (temp != null) {
                            log.warn("Jar包重复 -> {} | {}", id, file.getAbsolutePath());
                            return;
                        }
                        List<ClassDefinition> classDefinitions = new LinkedList<>();
                        for (Class<?> clazz : classLoader.findClassByConditionality(new String[]{packaging}, PluginDefinition.class)) {
                            PluginDefinition pluginDefinition = clazz.getAnnotation(PluginDefinition.class);
                            ClassDefinition classDefinition = new ClassDefinition(
                                    pluginDefinition.name(),
                                    clazz.getName(),
                                    jarDefinition.getVersion(),
                                    pluginDefinition.description(),
                                    pluginDefinition.type(),
                                    jarDefinition
                            );
                            classDefinitions.add(classDefinition);
                        }
                        jarDefinition.setClassDefinitions(classDefinitions.toArray(ClassDefinition[]::new));
                    } catch (Exception e) {
                        log.error("扫描Jar包失败", e);
                    }
                });
    }

    private String getJarId(JarDefinition jarDefinition) {
        return getJarId(jarDefinition.getPackaging(), jarDefinition.getVersion());
    }

    private String getJarId(String packaging, String version) {
        String id = String.join("/", packaging, version);
        return id;
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
        String id = getJarId(jarDefinition);
        JarDefinition temp = definitions.putIfAbsent(id, jarDefinition);
        if (temp != null) {
            log.warn("内置组件定义重复 -> {}", id);
            return;
        }
        List<ClassDefinition> classDefinitions = new LinkedList<>();
        for (Class<?> clazz : classLoader.findClassByConditionality(scanPaths, PluginDefinition.class)) {
            PluginDefinition pluginDefinition = clazz.getAnnotation(PluginDefinition.class);
            ClassDefinition classDefinition = new ClassDefinition(
                    pluginDefinition.name(),
                    clazz.getName(),
                    jarDefinition.getVersion(),
                    pluginDefinition.description(),
                    pluginDefinition.type(),
                    jarDefinition
            );
            classDefinitions.add(classDefinition);
        }
        jarDefinition.setClassDefinitions(classDefinitions.toArray(new ClassDefinition[0]));
        definitions.put(id, jarDefinition);
    }

    /**
     * 获取扫描到的Jar包
     *
     * @return
     */
    public Collection<JarDefinition> getJarDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    /**
     * 获取所有的定义类
     *
     * @return
     */
    public Collection<ClassDefinition> getClassDefinitions() {
        return definitions.values()
                .parallelStream()
                .flatMap(jarDefinition -> Arrays.stream(jarDefinition.getClassDefinitions()))
                .collect(Collectors.toList());
    }

    public boolean existJar(JarDefinition jarDefinition) {
        return existJar(jarDefinition.getPackaging(), jarDefinition.getVersion());
    }

    public boolean existJar(String packaging, String version) {
        return findJarByPackageAndVersion(packaging, version).isPresent();
    }

    public boolean existClass(ClassDefinition classDefinition) {
        return existClass(classDefinition.getClassName(), classDefinition.getVersion());
    }

    public boolean existClass(String classname, String version) {
        return findJarByClass(classname, version).isPresent();
    }

    public Optional<JarDefinition> findJarByPackageAndVersion(String packaging, String version) {
        return Optional.ofNullable(definitions.get(getJarId(packaging, version)));
    }

    public Optional<JarDefinition> findJarByClass(String classname, String version) {
        return definitions.values()
                .parallelStream()
                .filter(jarDefinition -> {
                    boolean exist = StringUtils.equals(version, jarDefinition.getVersion()) &&
                            classname.startsWith(jarDefinition.getPackaging());
                    if (exist) {
                        exist = Arrays.stream(jarDefinition.getClassDefinitions())
                                .parallel()
                                .anyMatch(classDefinition ->
                                        StringUtils.equals(classname, classDefinition.getClassName()) &&
                                                StringUtils.equals(version, classDefinition.getVersion()));
                    }
                    return exist;
                })
                .findFirst();
    }

    public Optional<ClassDefinition> findClassClassnameAndVersion(String classname, String version) {
        return definitions.values()
                .parallelStream()
                .flatMap(jarDefinition -> Arrays.stream(jarDefinition.getClassDefinitions()))
                .filter(classDefinition ->
                        StringUtils.equals(classname, classDefinition.getClassName()) &&
                                StringUtils.equals(version, classDefinition.getVersion()))
                .findFirst();
    }
}
