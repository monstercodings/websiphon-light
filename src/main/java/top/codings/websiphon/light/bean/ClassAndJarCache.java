package top.codings.websiphon.light.bean;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.error.FrameworkException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassAndJarCache {
    @Getter
    private String basePath;
    @Getter
    private Map<String, Map<String, String>> class2Jar = new ConcurrentHashMap<>();
    @Getter
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
                    realPath = basePath + "/" + path.replace(".", "/") + "/" + filename;
                }
                if (filename.endsWith(".jar")) {
                    String[] filenameStrs = filename.replace(".jar", "").split("-");
                    String version = filenameStrs[filenameStrs.length - 1];
                    JarFile jarFile = new JarFile(file);
                    try {
                        Enumeration<JarEntry> enumeration = jarFile.entries();
                        while (enumeration.hasMoreElements()) {
                            JarEntry entry = enumeration.nextElement();
                            if (!entry.getName().endsWith(".class")
//                                    || !entry.getName().startsWith(path == null ? "" : path)
                            ) {
                                continue;
                            }
                            String className = entry.getName()
                                    .replace("/", ".")
                                    .replace(".class", "");
                            Map<String, String> version2Jar = class2Jar.get(className);
                            if (null == version2Jar) {
                                version2Jar = new ConcurrentHashMap<>();
                                class2Jar.put(className, version2Jar);
                            }
                            version2Jar.put(version, realPath);
                        }
                    } finally {
                        jarFile.close();
                    }
                } else if (filename.endsWith(".class")) {
                    String className = filename.split("-")[0];
                    String version = filename.split("-")[1].replace(".class", "");
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
                next = String.format("%s.%s", path, file.getName());
            }
            scan(next, file.listFiles());
        }
    }

    public byte[] loadClassByte(String name, String version) {
        if (StringUtils.isBlank(name)) {
            throw new FrameworkException("类名不能为空");
        }
        String realPath;
        if (class2Path.containsKey(name)) {
            Map<String, String> version2Path = class2Path.get(name);
            realPath = version2Path.get(version);
            if (StringUtils.isBlank(realPath)) {
                throw new FrameworkException("该类名不存在");
            }
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(realPath))) {
                return bis.readAllBytes();
            } catch (Exception e) {
                throw new FrameworkException(String.format("读取本地依赖类失败 -> %s", realPath), e);
            }
        } else if (class2Jar.containsKey(name)) {
            Map<String, String> version2Jar = class2Jar.get(name);
            realPath = version2Jar.get(version);
            if (StringUtils.isBlank(realPath)) {
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
}
