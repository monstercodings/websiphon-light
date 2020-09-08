package top.codings.websiphon.light.crawler.support;

import top.codings.websiphon.light.config.RegistryConfig;
import top.codings.websiphon.light.crawler.CombineCrawler;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.crawler.RegistrableCrawler;
import top.codings.websiphon.light.function.registry.IRegistry;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<? extends ICrawler> startup() {
        CompletableFuture<? extends ICrawler> completableFuture = super.startup();
        if (registryConfig.isEnabled()) {
            registry.setConfig(config, registryConfig);
            registry.setCrawler(this);
            return completableFuture.thenCombineAsync(registry.startup(), (crawler, iRegistry) -> crawler);
        }
        return completableFuture;
    }

    @Override
    public CompletableFuture<ICrawler> shutdown() {
        CompletableFuture cf = new CompletableFuture();
        cf.completeAsync(() -> null);
        if (null != registry) {
            cf = registry.shutdown(true);
        }
        return CompletableFuture.allOf(cf, super.shutdown()).thenApplyAsync(o -> this.wrapper());
    }

    @Override
    public IRegistry registry() {
        return registry;
    }
}
