package top.codings.websiphon.light.bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebsiphonClassLoader extends ClassLoader {
    private JarCache jarCache;
    private Map<String, Class<?>> cacheClass = new ConcurrentHashMap<>();

    public WebsiphonClassLoader(String basePath) {
        this(basePath, null);
    }

    public WebsiphonClassLoader(JarCache jarCache) {
        this(null, jarCache);
    }

    public WebsiphonClassLoader(String basePath, JarCache jarCache) {
        if (null == jarCache) {
            jarCache = new JarCache(basePath);
        }
        this.jarCache = jarCache;
    }

    public Class<?> loadClass(String name, String version) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return loadClassFromByte(name, jarCache.loadClassByte(name, version));
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (cacheClass.containsKey(name)) {
            return cacheClass.get(name);
        }
        return super.findClass(name);
    }

    public Class<?> loadClassFromByte(String name, byte[] bytes) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
            cacheClass.put(name, clazz);
            return clazz;
        }

    }
}
