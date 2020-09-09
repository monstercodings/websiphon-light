package top.codings.websiphon.light.loader;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.loader.bean.ClassDefinition;
import top.codings.websiphon.light.loader.bean.JarDefinition;
import top.codings.websiphon.light.loader.bean.PluginType;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
public class CacheDefinitionContext {
    @Getter
    private volatile PluginDefinitionLoader loader;
    private String basePath;
    private String ownPackage;
    @Getter
    private Collection<ClassDefinition> requesterDefinition;
    @Getter
    private Collection<ClassDefinition> handlerDefinition;
    @Getter
    private Collection<ClassDefinition> processorDefinition;

    public CacheDefinitionContext(String basePath, String ownPackage) throws Exception {
        this.basePath = basePath;
        this.ownPackage = ownPackage;
        refresh();
    }

    public Collection<JarDefinition> getAllJarDefinition() {
        return Collections.unmodifiableCollection(loader.getJarDefinitions());
    }

    public boolean existJar(JarDefinition query) {
        Collection<JarDefinition> jarDefinitions = loader.getJarDefinitions();
        String name = query.getName(), version = query.getVersion(), packaging = query.getPackaging();
        return jarDefinitions.parallelStream()
                .filter(jarDefinition ->
                        StringUtils.equals(name, jarDefinition.getName()) &&
                                StringUtils.equals(version, jarDefinition.getVersion()) &&
                                StringUtils.equals(packaging, jarDefinition.getPackaging()))
                .toArray().length >= 1;
    }

    private Collection<ClassDefinition> getProcessorPlugins() {
        Collection<JarDefinition> jarDefinitions = loader.getJarDefinitions();
        return jarDefinitions.parallelStream()
                .flatMap(jarDefinition -> Arrays.stream(jarDefinition.getClassDefinitions()))
                .filter(classDefinition -> classDefinition.getType() == PluginType.PROCESSOR)
                .collect(Collectors.toList());
    }

    private Collection<ClassDefinition> getRequesterPlugins() {
        Collection<JarDefinition> jarDefinitions = loader.getJarDefinitions();
        return jarDefinitions.parallelStream()
                .flatMap(jarDefinition -> Arrays.stream(jarDefinition.getClassDefinitions()))
                .filter(classDefinition -> classDefinition.getType() == PluginType.REQUESTER)
                .collect(Collectors.toList());
    }

    private Collection<ClassDefinition> getHandlerPlugins() {
        Collection<JarDefinition> jarDefinitions = loader.getJarDefinitions();
        return jarDefinitions.parallelStream()
                .flatMap(jarDefinition -> Arrays.stream(jarDefinition.getClassDefinitions()))
                .filter(classDefinition -> classDefinition.getType() == PluginType.HANDLER)
                .collect(Collectors.toList());
    }

    public void save(JarDefinition jarDefinition, String filename, byte[] bytes) throws Exception {
        String packaging = jarDefinition.getPackaging().replace(".", "/");
        File file = Path.of(basePath, packaging, jarDefinition.getVersion(), filename).toFile();
        if (!file.getParentFile().exists()) {
            FileUtils.forceMkdirParent(file);
        }
        FileUtils.writeByteArrayToFile(file, bytes);
        refresh();
    }

    public void refresh() throws Exception {
        synchronized (this) {
            File file = Path.of(basePath).toFile();
            if (!file.exists()) {
                FileUtils.forceMkdir(file);
            }
            loader = new PluginDefinitionLoader(basePath, ownPackage);
            requesterDefinition = getRequesterPlugins();
            handlerDefinition = getHandlerPlugins();
            processorDefinition = getProcessorPlugins();
        }
    }
}
