package top.codings.websiphon.light.loader.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginDefinition {
    /**
     * 是否为整体Jar包描述对象
     *
     * @return
     */
    boolean primary() default false;

    /**
     * 插件名字
     *
     * @return
     */
    String name();

    /**
     * 插件的描述/作用
     *
     * @return
     */
    String description();

    /**
     * 插件的版本号
     * 只对primary为true的类有效
     *
     * @return
     */
    String version() default "";
}
