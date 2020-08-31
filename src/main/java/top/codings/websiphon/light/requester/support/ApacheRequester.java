package top.codings.websiphon.light.requester.support;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DeflateInputStreamFactory;
import org.apache.http.client.entity.GZIPInputStreamFactory;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import top.codings.websiphon.light.config.RequesterConfig;
import top.codings.websiphon.light.error.FrameworkException;
import top.codings.websiphon.light.function.handler.IResponseHandler;
import top.codings.websiphon.light.loader.anno.PluginDefinition;
import top.codings.websiphon.light.loader.bean.PluginType;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.requester.IRequester;
import top.codings.websiphon.light.utils.HttpCharsetUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDefinition(name = "Apache请求器", description = "基于Apache的Httpclient定制化的请求器，支持Apache httpclient的特性", version = "0.0.1", type = PluginType.REQUESTER)
public class ApacheRequester extends CombineRequester<ApacheRequest> {
    @Getter
    @Setter
    private IResponseHandler responseHandler;
    private RequesterConfig config;

    private String contentTypePattern = "([a-z]+/[^;\\.]+);?\\s?(charset=)?(.*)";
    private Pattern pattern = Pattern.compile(contentTypePattern, Pattern.CASE_INSENSITIVE);
    private CloseableHttpAsyncClient client;
    private RequestConfig requestConfig;
    private Registry<InputStreamFactory> decoderRegistry;

    public ApacheRequester() {
        this(null);
    }

    public ApacheRequester(RequesterConfig config) {
        super(null);
        if (config == null) {
            config = RequesterConfig.builder()
                    .connectTimeoutMillis(30000)
                    .idleTimeMillis(30000)
                    .maxContentLength(Integer.MAX_VALUE)
                    .networkErrorStrategy(NetworkErrorStrategy.RESPONSE)
                    .ignoreSslError(false)
                    .redirect(false)
                    .build();
        }
        setStrategy(config.getNetworkErrorStrategy());
        this.config = config;
    }

    @Override
    public CompletableFuture<IRequester> init() {
        try {
            decoderRegistry = RegistryBuilder.<InputStreamFactory>create()
                    .register("gzip", GZIPInputStreamFactory.getInstance())
                    .register("x-gzip", GZIPInputStreamFactory.getInstance())
                    .register("deflate", DeflateInputStreamFactory.getInstance())
                    .build();
            HttpHost httpHost = null;
            if (config.getProxy() != null) {
                Proxy proxy = config.getProxy();
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                httpHost = HttpHost.create(
                        String.format("%s://%s:%d",
                                proxy.type().name().toLowerCase(),
                                address.getHostString(),
                                address.getPort()
                        )
                );
            }
            requestConfig = RequestConfig
                    .custom()
                    .setProxy(httpHost)
                    .setContentCompressionEnabled(true)
                    .setRedirectsEnabled(config.isRedirect())
                    .setRelativeRedirectsAllowed(config.isRedirect())
                    .setCircularRedirectsAllowed(false)
                    .build();
            HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClients
                    .custom()
                    .setConnectionReuseStrategy(DefaultClientConnectionReuseStrategy.INSTANCE)
                    .disableCookieManagement()
                    .setDefaultRequestConfig(requestConfig)
                    .setMaxConnPerRoute(Integer.MAX_VALUE)
                    .setMaxConnTotal(Integer.MAX_VALUE);
            if (config.isIgnoreSslError()) {
                httpAsyncClientBuilder.setSSLStrategy(new SSLIOSessionStrategy(
                        SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build(),
                        (hostname, session) -> true)
                );
            }
            client = httpAsyncClientBuilder.build();
            client.start();
        } catch (Exception e) {
//            log.error("初始化请求器失败", e);
            return CompletableFuture.failedFuture(new FrameworkException("初始化请求器失败", e));
        }
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public CompletableFuture<ApacheRequest> execute(ApacheRequest request) {
        HttpUriRequest httpRequest = request.getHttpRequest();
        HttpClientContext context = HttpClientContext.create();
//        context.setAttribute("webRequest", request);
        RequestConfig.Builder builder = RequestConfig
                .copy(requestConfig)
                .setSocketTimeout(config.getConnectTimeoutMillis())
                .setConnectTimeout(config.getConnectTimeoutMillis())
                .setConnectionRequestTimeout(config.getIdleTimeMillis());
        // TODO 此处支持代理
        /*WebProxy proxy = httpRequest.getProxy();
        if (proxy != null && proxy != WebProxy.NO_PROXY) {
            builder.setProxy(new HttpHost(proxy.getProxyIp(), proxy.getProxyPort()));
        }*/
        CompletableFuture<ApacheRequest> completableFuture = new CompletableFuture();
        context.setRequestConfig(builder.build());
        client.execute(httpRequest, context, new CustomFutureCallback(request, completableFuture));
        return completableFuture;
    }

    @Override
    public ApacheRequest create(String url) {
        return create(url, null);
    }

    @Override
    public ApacheRequest create(String url, Object userData) {
        return new ApacheRequest(new HttpGet(url), userData);
    }

    @Override
    public CompletableFuture<IRequester> shutdown(boolean force) {
        if (null != client) {
            try {
                client.close();
            } catch (IOException e) {
                log.error("关闭HTTP请求器异常", e);
            }
        }
        return CompletableFuture.completedFuture(this);
    }

    private interface Task<T> {
        void handle(T t) throws Exception;
    }

    @AllArgsConstructor
    private class CustomFutureCallback implements FutureCallback<HttpResponse> {
        private ApacheRequest request;
        private CompletableFuture completableFuture;


        private void verifyStatus(Task task) {
            request.lock();
            try {
                if (null != request.requestResult) {
                    return;
                }
                request.setRequestResult(new IRequest.RequestResult());
                if (request.getStatus() == IRequest.Status.TIMEOUT) {
                    request.requestResult.setSucceed(false);
                    request.requestResult.setThrowable(new RuntimeException("该任务请求已超时，取消业务处理"));
                    return;
                }
                request.setStatus(IRequest.Status.RESPONSE);
                task.handle(null);
            } catch (Exception e) {
                // 发生处理响应异常之后，取消业务处理，直接返回失败结果
                request.requestResult.setSucceed(false);
                request.requestResult.setThrowable(e);
            } finally {
                request.unlock();
                completableFuture.completeAsync(() -> request);
            }
        }

        @Override
        public void completed(HttpResponse result) {
            verifyStatus(obj -> {
                try {
                    byte[] body = EntityUtils.toByteArray(result.getEntity());
                    final Header ceheader = result.getEntity().getContentEncoding();
                    if (ceheader != null) {
                        final HeaderElement[] codecs = ceheader.getElements();
                        for (final HeaderElement codec : codecs) {
                            final String codecname = codec.getName().toLowerCase(Locale.ROOT);
                            final InputStreamFactory decoderFactory = decoderRegistry.lookup(codecname);
                            if (decoderFactory != null) {
                                try (InputStream is = decoderFactory.create(new ByteArrayInputStream(body))) {
                                    body = IOUtils.toByteArray(is);
                                }
                            }
                        }
                    }
                    request.requestResult.setCode(result.getStatusLine().getStatusCode());
                    request.requestResult.setSucceed(true);
                    String mimeType = "text/html";
                    Charset charset = null;
                    Header ct = result.getFirstHeader("content-type");
                    String contentType;
                    if (ct != null) {
                        contentType = ct.getValue();
                        Matcher matcher = pattern.matcher(contentType);
                        if (matcher.find()) {
                            mimeType = matcher.group(1);
                            if (StringUtils.isBlank(mimeType)) {
                                mimeType = "text/html";
                            }
                            String charsetStr = matcher.group(3);
                            if (StringUtils.isNotBlank(charsetStr)) {
                                if (charsetStr.contains(",")) {
                                    charsetStr = charsetStr.split(",")[0];
                                }
                                charset = CharsetUtils.lookup(charsetStr);
                            }
                        } else {
                            log.trace("无字符编码类型[{}] -> {}", contentType, request.getHttpRequest().getURI().toString());
                            mimeType = "text/html";
                        }
                    }
                    if (null == charset) {
                        charset = HttpCharsetUtil.findCharset(body);
                        if (null == charset) {
                            charset = Charset.defaultCharset();
                        }
                    }
                    if (mimeType.contains("text")) {
                        // 文本解析
                        request.requestResult.setResponseType(IRequest.ResponseType.TEXT);
                        request.requestResult.setData(Optional.ofNullable(new String(body, charset)).orElse("<html>该网页无内容</html>"));
                    } else if (mimeType.contains("json")) {
                        // JSON解析
                        request.requestResult.setResponseType(IRequest.ResponseType.JSON);
                        request.requestResult.setData(JSON.parse(Optional.ofNullable(new String(body, charset)).orElse("{}")));
                    } else {
                        // 字节解析
                        request.requestResult.setResponseType(IRequest.ResponseType.BYTE);
                        request.requestResult.setData(body);
                    }
                    if (null != responseHandler) {
                        responseHandler.handle(request);
                    }
                } finally {
                    HttpClientUtils.closeQuietly(result);
                }
            });

            /*request.setStatus(IRequest.Status.RESPONSE);
            IRequest.RequestResult requestResult = new IRequest.RequestResult();
            request.setRequestResult(requestResult);
            try {
//                WebDownloadEvent event = new WebDownloadEvent(ContentType.getOrDefault(result.getEntity()), result.getEntity().getContentLength());
                byte[] body = EntityUtils.toByteArray(result.getEntity());
                final Header ceheader = result.getEntity().getContentEncoding();
                if (ceheader != null) {
                    final HeaderElement[] codecs = ceheader.getElements();
                    for (final HeaderElement codec : codecs) {
                        final String codecname = codec.getName().toLowerCase(Locale.ROOT);
                        final InputStreamFactory decoderFactory = decoderRegistry.lookup(codecname);
                        if (decoderFactory != null) {
                            try (InputStream is = decoderFactory.create(new ByteArrayInputStream(body))) {
                                body = IOUtils.toByteArray(is);
                            }
                        }
                    }
                }
                request.setHttpResponse(result);
                requestResult.setSucceed(true);
                String mimeType = "text/html";
                Charset charset = null;
                Header ct = result.getFirstHeader("content-type");
                String contentType;
                if (ct != null) {
                    contentType = ct.getValue();
                    Matcher matcher = pattern.matcher(contentType);
                    if (matcher.find()) {
                        mimeType = matcher.group(1);
                        if (StringUtils.isBlank(mimeType)) {
                            mimeType = "text/html";
                        }
                        String charsetStr = matcher.group(3);
                        if (StringUtils.isNotBlank(charsetStr)) {
                            if (charsetStr.contains(",")) {
                                charsetStr = charsetStr.split(",")[0];
                            }
                            charset = CharsetUtils.lookup(charsetStr);
                        }
                    } else {
                        log.trace("无字符编码类型[{}] -> {}", contentType, request.getHttpRequest().getURI().toString());
                        mimeType = "text/html";
                    }
                }
                if (null == charset) {
                    charset = HttpCharsetUtil.findCharset(body);
                }
                if (mimeType.contains("text")) {
                    // 文本解析
                    request.requestResult.setResponseType(IRequest.ResponseType.TEXT);
                    request.requestResult.setData(Optional.ofNullable(new String(body, charset)).orElse("<html>该网页无内容</html>"));
                } else if (mimeType.contains("json")) {
                    // JSON解析
                    request.requestResult.setResponseType(IRequest.ResponseType.JSON);
                    request.requestResult.setData(JSON.parse(Optional.ofNullable(new String(body, charset)).orElse("{}")));
                } else {
                    // 字节解析
                    request.requestResult.setResponseType(IRequest.ResponseType.UNKNOW);
                    request.requestResult.setData(body);
                }
                responseHandler.push(request);
                completableFuture.completeAsync(() -> request);
            } catch (Exception e) {
                failed(e);
            } finally {
//                HttpClientUtils.closeQuietly(result);
            }*/
        }

        @Override
        public void failed(Exception ex) {
            verifyStatus(o -> {
                request.requestResult.setSucceed(false);
                request.requestResult.setThrowable(ex);
                if (getStrategy() == IRequester.NetworkErrorStrategy.DROP) {
                    return;
                }
                if (null != responseHandler) {
                    responseHandler.handle(request);
                }
            });
        }

        @Override
        public void cancelled() {
            verifyStatus(o -> {
                request.requestResult.setSucceed(false);
                request.requestResult.setThrowable(new RuntimeException("请求被取消"));
                if (getStrategy() == IRequester.NetworkErrorStrategy.DROP) {
                    return;
                }
                if (null != responseHandler) {
                    responseHandler.handle(request);
                }
            });
        }
    }

}
