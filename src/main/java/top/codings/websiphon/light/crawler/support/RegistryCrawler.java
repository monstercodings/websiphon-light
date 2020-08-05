package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.config.RegistryConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.RegistrableCrawler;
import top.codings.websiphon.light.manager.IRegistry;

import java.util.Optional;

public class RegistryCrawler extends CombineCrawler implements RegistrableCrawler {
    private RegistryConfig registryConfig;
    private IRegistry registry;

    public RegistryCrawler(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
        if (registryConfig.isEnabled()) {
            // 初始化注册中心配置
            try {
                // 初始化注册器
                registry = (IRegistry) Class
                        .forName(
                                registryConfig.getRegistryImplClass(),
                                true,
                                Optional.ofNullable(registryConfig.getClassLoader()).orElse(this.getClass().getClassLoader())
                        )
                        .getConstructor()
                        .newInstance();
            } catch (Exception e) {
                throw new RuntimeException("初始化注册器失败", e);
            }
        }
    }

    @Override
    public void startup() {
        if (registryConfig.isEnabled()) {
            registry.setConfig(config, registryConfig);
            registry.setCrawler(this);
            registry.startup();
        }
        super.startup();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (null != registry) {
            registry.shutdown(true);
        }
    }

    @Override
    public IRegistry registry() {
        return registry;
    }
}
