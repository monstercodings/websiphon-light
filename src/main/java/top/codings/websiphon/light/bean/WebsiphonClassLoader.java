package top.codings.websiphon.light.bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebsiphonClassLoader extends ClassLoader {
    private ClassAndJarCache classAndJarCache;
    private Map<String, Class<?>> cacheClass = new ConcurrentHashMap<>();

    public WebsiphonClassLoader(String basePath) {
        this(basePath, null);
    }

    public WebsiphonClassLoader(ClassAndJarCache classAndJarCache) {
        this(null, classAndJarCache);
    }

    public WebsiphonClassLoader(String basePath, ClassAndJarCache classAndJarCache) {
        if (null == classAndJarCache) {
            classAndJarCache = new ClassAndJarCache(basePath);
        }
        this.classAndJarCache = classAndJarCache;
    }

    public Class<?> loadClass(String name, String version) {
        try {
            return loadClass(name);
        } catch (ClassNotFoundException e) {
            return loadClassFromByte(name, classAndJarCache.loadClassByte(name, version), true);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (cacheClass.containsKey(name)) {
            return cacheClass.get(name);
        }
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            return loadClassFromByte(name, classAndJarCache.loadClassByte(name, null), true);
        }
    }

    public Class<?> loadClassFromByte(String name, byte[] bytes) {
        return loadClassFromByte(name, bytes, false);
    }

    public Class<?> loadClassFromByte(String name, byte[] bytes, boolean direct) {
        if (direct) {
            Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
            cacheClass.put(name, clazz);
            return clazz;
        }
        try {
            return loadClass(name);
        } catch (ClassNotFoundException e) {
            Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
            cacheClass.put(name, clazz);
            return clazz;
        }

    }
}
