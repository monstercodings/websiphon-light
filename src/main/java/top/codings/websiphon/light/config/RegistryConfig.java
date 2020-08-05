package top.codings.websiphon.light.config;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegistryConfig {
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 注册中心地址
     */
    private String registryUrl;
    /**
     * 注册API
     */
    private String registryApi;
    /**
     * 拉取任务API
     */
    private String pullTaskApi;
    /**
     * 注册器的全限定类名
     */
    private String registryImplClass;
    /**
     * 注册器的加载器
     */
    private ClassLoader classLoader;
}
