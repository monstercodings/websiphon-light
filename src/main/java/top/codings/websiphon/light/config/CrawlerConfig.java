package top.codings.websiphon.light.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.light.crawler.ICrawler;
import top.codings.websiphon.light.requester.IRequester;

import java.util.function.Consumer;

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
     * 最大处理响应线程数
     */
    @Setter
    private int maxConcurrentProcessing;
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
    /**
     * 网络异常时的请求对象的处理策略
     */
    private IRequester.NetworkErrorStrategy networkErrorStrategy;
    /**
     * 爬虫关闭前的回调函数
     */
    private Consumer<ICrawler> shutdownHook;
}
