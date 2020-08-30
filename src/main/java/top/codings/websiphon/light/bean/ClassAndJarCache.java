package top.codings.websiphon.light.bean;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.error.FrameworkException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Deprecated
public class ClassAndJarCache {
    @Getter
    private String basePath;
    private Map<String, Map<String, String>> class2Jar = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> class2Path = new ConcurrentHashMap<>();

    public ClassAndJarCache(String basePath) {
        if (StringUtils.isBlank(basePath)) {
            throw new FrameworkException("基础路径不能为空");
        }
        this.basePath = basePath;
        File dir = new File(basePath);
        try {
            scan(null, dir.listFiles());
        } catch (IOException e) {
            throw new FrameworkException("扫描基础路径下的依赖包失败", e);
        }
    }

    private void scan(String path, File[] files) throws IOException {
        for (File file : files) {
            if (!file.isDirectory()) {
                String filename = file.getName();
                String realPath;
                if (StringUtils.isBlank(path)) {
                    realPath = basePath + "/" + filename;
                } else {
//                    realPath = basePath + "/" + path.replace(".", "/") + "/" + filename;
                    realPath = String.join("/", basePath, path, filename);
                }
                if (filename.endsWith(".jar")) {
                    String[] pathArray = path.split("/");
                    String version = pathArray[pathArray.length - 1];
                    try (JarFile jarFile = new JarFile(file)) {
                        Enumeration<JarEntry> enumeration = jarFile.entries();
                        while (enumeration.hasMoreElements()) {
                            JarEntry entry = enumeration.nextElement();
                            if (!entry.getName().endsWith(".class")
//                                    || !entry.getName().startsWith(path == null ? "" : path)
                            ) {
                                continue;
                            }
                            String className = entry.getName()
                                    .replace(".class", "")
                                    .replace("/", ".");
                            Map<String, String> version2Jar = class2Jar.get(className);
                            if (null == version2Jar) {
                                version2Jar = new ConcurrentHashMap<>();
                                class2Jar.put(className, version2Jar);
                            }
                            version2Jar.put(version, realPath);
                        }
                    }
                } else if (filename.endsWith(".class")) {
                    /*String className = filename.split("-")[0];
                    String version = filename.split("-")[1].replace(".class", "");*/
                    String className = filename.replace(".class", "");
                    String[] pathArray = path.split("/");
                    String version = pathArray[pathArray.length - 1];
                    Map<String, String> version2Path = class2Path.get(className);
                    if (null == version2Path) {
                        version2Path = new ConcurrentHashMap<>();
                        class2Path.put(className, version2Path);
                    }
                    version2Path.put(version, realPath);
                }
                continue;
            }
            String next;
            if (StringUtils.isBlank(path)) {
                next = file.getName();
            } else {
                next = String.format("%s/%s", path, file.getName());
            }
            scan(next, file.listFiles());
        }
    }

    public byte[] loadClassByte(String name, String version) {
        if (StringUtils.isAnyBlank(name, version)) {
            throw new FrameworkException("类名不能为空");
        }
        String realPath;
        if (class2Path.containsKey(name)) {
            Map<String, String> version2Path = class2Path.get(name);
            // TODO 此处临时解决，需要更好的解决方案
            /*if (version2Path.size() == 1) {
                realPath = version2Path.values().stream().findFirst().get();
            } else {
                realPath = version2Path.get(version);
            }*/
            if (StringUtils.isBlank((realPath = version2Path.get(version)))) {
                throw new FrameworkException("该类名不存在");
            }
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(realPath))) {
                return bis.readAllBytes();
            } catch (Exception e) {
                throw new FrameworkException(String.format("读取本地依赖类失败 -> %s", realPath), e);
            }
        } else if (class2Jar.containsKey(name)) {
            Map<String, String> version2Jar = class2Jar.get(name);
            // TODO 此处临时解决，需要更好的解决方案
            /*if (version2Jar.size() == 1) {
                realPath = version2Jar.values().stream().findFirst().get();
            } else {
                realPath = version2Jar.get(version);
            }*/
            if (StringUtils.isBlank(realPath = version2Jar.get(version))) {
                throw new FrameworkException("该类名不存在");
            }
            try {
                JarFile jarFile = new JarFile(realPath);
                try (BufferedInputStream bis = new BufferedInputStream(
                        jarFile.getInputStream(
                                jarFile.getEntry(name.replace(".", "/") + ".class")))) {
                    return bis.readAllBytes();
                }
            } catch (IOException e) {
                throw new FrameworkException(String.format("读取本地依赖包失败 -> %s", realPath), e);
            }
        } else {
            throw new FrameworkException("该类名不存在");
        }
    }

    /**
     * 通过jar路径读取里面的所有类
     *
     * @param jarPath
     * @return
     */
    public Map<String, byte[]> loadJar(String jarPath) {
        Map<String, byte[]> map = new HashMap<>();
        try (JarFile jarFile = new JarFile(jarPath)) {
            jarFile.stream().forEach(entry -> {
                if (!entry.getName().endsWith(".class")) return;
                String className = entry.getName()
                        .replace(".class", "")
                        .replace("/", ".");
                try (BufferedInputStream bis = new BufferedInputStream(jarFile.getInputStream(entry))) {
                    map.put(className, bis.readAllBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return map;
        } catch (IOException e) {
            throw new FrameworkException("读取本地缓存jar包失败");
        }
    }

    /**
     * 通过类名和版本号查找对应的jar路径
     *
     * @param classname
     * @param version
     * @return
     */
    public Optional<String> findJarByClassnameAndVersion(String classname, String version) {
        if (!class2Jar.containsKey(classname)) {
            return Optional.empty();
        }
        Map<String, String> version2Jar = class2Jar.get(classname);
        if (!version2Jar.containsKey(version)) {
            return Optional.empty();
        }
        return Optional.ofNullable(version2Jar.get(version));
    }
}
