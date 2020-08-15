package top.codings.websiphon.light.bean;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

public class WebsiphonClassLoader extends URLClassLoader {
    private final static String BASE_PATH = new File("").getAbsolutePath().concat("/config/compiler/");
    private static URL[] baseUrls;

    static {
        try {
            baseUrls = new URL[]{
                    new URL("file://" + BASE_PATH),
            };
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public WebsiphonClassLoader(URL[] urls) {
        super(urls);
    }


    public WebsiphonClassLoader() {
        this(baseUrls);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            return findClass0(name);
        }
    }

    private Class<?> findClass0(String name) throws ClassNotFoundException {
        String fullPath = BASE_PATH.concat(name.replace(".", "/").concat(".class"));
        try {
            byte[] bytes = IOUtils.toByteArray(URI.create("file://" + fullPath));
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("读取Class文件失败", e);
        }
    }
}
