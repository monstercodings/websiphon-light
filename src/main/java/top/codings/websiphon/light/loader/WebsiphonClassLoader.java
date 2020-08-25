package top.codings.websiphon.light.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

@Slf4j
public class WebsiphonClassLoader extends URLClassLoader {
    private final static Object VALUE = new Object();

    public WebsiphonClassLoader(URL[] urls) {
        super(urls);
    }

    public Class<?>[] findAllClass(String[] packageNames) {
        return findClassByConditionality(packageNames, null);
    }

    public Class<?>[] findClassByConditionality(String[] packageNames, Class<? extends Annotation> annotationClass) {
        Map<Class<?>, Object> classes = new ConcurrentHashMap<>();
        try {
            for (String packageName : packageNames) {
                if (StringUtils.equals(packageName, null)) {
                    continue;
                }
                String packagePath = packageName.replace(".", "/");
//                findClasses(annotationClass, classes, packagePath, this.getClass().getClassLoader().getResources(packagePath));
                findClasses(annotationClass, classes, packagePath, getResources(packagePath));
            }
            return classes.keySet().toArray(new Class<?>[0]);
        } catch (IOException e) {
            log.error("获取类失败", e);
            return new Class<?>[0];
        }
    }

    private void findClasses(Class<? extends Annotation> annotationClass, Map<Class<?>, Object> classes, String packagePath, Enumeration<URL> enumeration) throws IOException {
        if (enumeration.hasMoreElements() && log.isTraceEnabled()) {
            log.trace("当前扫描包 -> {}", packagePath.replace("/", "."));
        }
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            String protocol = url.getProtocol();
            if (protocol.equalsIgnoreCase("jar")) {
                try (JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Jar包名称 -> {}", jarFile.getName());
                    }
                    jarFile.stream().parallel()
                            .filter(entry -> entry.getName().endsWith(".class") && entry.getName().startsWith(packagePath))
                            .forEach(entry -> {
                                String className = entry.getName()
                                        .replace(".class", "")
                                        .replace("/", ".");
                                findClassByConditionality(annotationClass, classes, className);
                            });
                }
            } else if (protocol.equalsIgnoreCase("file")) {
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                FileUtils.listFiles(new File(filePath), new String[]{"class"}, true).parallelStream()
                        .filter(file ->
                                file.toURI().getPath()
                                        .replace(filePath.replace(packagePath, ""), "")
                                        .startsWith(packagePath))
                        .forEach(file -> {
//                    System.out.println(
//                            file.toURI().getPath()
//                                    .replace(filePath.replace(packagePath, ""), "")
//                    );
                            String className = file.toURI().getPath()
                                    .replace(filePath.replace(packagePath, ""), "")
                                    .replace(".class", "").replace("/", ".");
                            findClassByConditionality(annotationClass, classes, className);
                        });
            }
        }
    }

    private void findClassByConditionality(Class<? extends Annotation> annotationClass, Map<Class<?>, Object> classes, String className) {
        try {
            Class clazz = loadClass(className);
            if (annotationClass == null) {
                classes.put(clazz, VALUE);
            } else if (clazz.getAnnotation(annotationClass) != null) {
                classes.put(clazz, VALUE);
            }
        } catch (Exception e) {
            log.error("无法找到该类 -> {}", className, e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return this.getClass().getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            return super.findClass(name);
        }
    }
}
