package top.codings.websiphon.light.bean;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebsiphonClassLoader extends URLClassLoader {
    /*private final static String BASE_PATH = new File("").getAbsolutePath().concat("/config/compiler/");
    private static URL[] baseUrls;*/
    private Map<String, Class<?>> cacheClass = new ConcurrentHashMap<>();

    /*static {
        try {
            baseUrls = new URL[]{
                    new URL("file://" + BASE_PATH),
            };
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }*/

    public WebsiphonClassLoader(URL[] urls) {
        super(urls);
    }


    public WebsiphonClassLoader() {
        this(new URL[0]);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (cacheClass.containsKey(name)) {
            return cacheClass.get(name);
        }
        return super.findClass(name);
    }

    /*private Class<?> findClass0(String name) throws ClassNotFoundException {
        String fullPath = BASE_PATH.concat(name.replace(".", "/").concat(".class"));
        try {
            byte[] bytes = IOUtils.toByteArray(URI.create("file://" + fullPath));
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("读取Class文件失败", e);
        }
    }*/

    public Class<?> loadClassFromByte(String name, byte[] bytes) {
        Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
        cacheClass.put(name, clazz);
        return clazz;
    }
}
