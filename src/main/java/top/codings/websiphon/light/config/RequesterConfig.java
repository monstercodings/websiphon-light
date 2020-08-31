package top.codings.websiphon.light.config;

import lombok.Builder;
import lombok.Getter;
import top.codings.websiphon.light.requester.IRequester;

import java.net.Proxy;

@Getter
@Builder
public class RequesterConfig {
    /**
     * 连接超时时间
     */
    private int connectTimeoutMillis;
    /**
     * 传输超时设定
     */
    private int idleTimeMillis;
    /**
     * 是否忽略ssl错误
     */
    private boolean ignoreSslError;
    /**
     * 是否允许跳转
     */
    private boolean redirect;
    /**
     * 最大允许接受的响应长度
     */
    private int maxContentLength;
    /**
     * 上传限速
     * 单位字节
     */
    private int uploadBytesPerSecond;
    /**
     * 下载限速
     * 单位字节
     */
    private int downloadBytesPerSecond;
    /**
     * 代理对象
     */
    private Proxy proxy;
    /**
     * 网络异常时的请求对象的处理策略
     */
    private IRequester.NetworkErrorStrategy networkErrorStrategy;
}
