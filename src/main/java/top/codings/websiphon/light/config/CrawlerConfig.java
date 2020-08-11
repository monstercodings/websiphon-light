package top.codings.websiphon.light.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class CrawlerConfig {
    /**
     * 爬虫名字
     */
    private String name;
    /**
     * 爬虫版本
     */
    private String version;
    /**
     * 是否使用异步模式
     */
    private boolean sync;
    /**
     * 最大处理响应线程数
     */
    @Setter
    private int maxConcurrentProcessing;
    /**
     * 最大网络并发数
     */
    private int maxNetworkConcurrency;
    /**
     * 响应处理器的全限定类名
     */
    private String responseHandlerImplClass;
    /**
     * 请求器的全限定类名
     */
    private String requesterClass;
    /**
     * 响应处理器的加载器
     */
    private ClassLoader classLoader;
}
