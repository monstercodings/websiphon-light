package top.codings.websiphon.light.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.light.bean.TaskResult;
import top.codings.websiphon.light.error.FrameworkException;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.util.Arrays;

@Slf4j
public abstract class CompilerUtil {
    private CompilerUtil(){}

    private static final String COMPILER_PATH = "config/compiler/";
    private static final String BASE_PATH = new File("").getAbsolutePath()
            .concat("/").concat(COMPILER_PATH);
    private static final JavaCompiler JAVA_COMPILER = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager STANDARD_JAVA_FILE_MANAGER = JAVA_COMPILER.getStandardFileManager(null, null, null);
    private static URL[] urls;

    static {
        try {
            File file = new File(COMPILER_PATH + "temp");
            FileUtils.forceMkdirParent(file);
            urls = new URL[]{
                    new URL("file://".concat(BASE_PATH))};
        } catch (Exception e) {
            log.error("初始化本地Class的URL失败", e);
        }
    }

    public static final TaskResult compiler(String className) {
        if (StringUtils.isBlank(className)) {
            return new TaskResult<>(false,
                    new IllegalArgumentException("全限定类名不能为空"),
                    null);
        }
        String path = className.replace(".", "/") + ".java";
        return compiler(new File(COMPILER_PATH + path), className);
    }

    private static final TaskResult compiler(File file, String className) {
        Iterable<? extends JavaFileObject> javaFileObjects = STANDARD_JAVA_FILE_MANAGER.getJavaFileObjects(file);
        JavaCompiler.CompilationTask task = JAVA_COMPILER.getTask(null, STANDARD_JAVA_FILE_MANAGER,
                null, Arrays.asList(
                        "-implicit:class"
//                        "-classpath config/compiler/websiphon-light.jar"
                ), null, javaFileObjects);
        boolean compileFlag = task.call();
        if (!compileFlag) {
            return new TaskResult<>(false,
                    new FrameworkException(String.format("编译[%s]失败", file.getName())),
                    null
            );
        }
        return new TaskResult<>(true, null, null);
    }
}
