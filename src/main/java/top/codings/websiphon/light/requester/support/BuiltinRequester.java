package top.codings.websiphon.light.requester.support;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.CharsetUtils;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;
import top.codings.websiphon.light.utils.HttpCharsetUtil;

import javax.net.ssl.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BuiltinRequester extends CombineRequester<BuiltinRequest> implements AsyncRequester<BuiltinRequest> {
    @Setter
    @Getter
    private QueueResponseHandler responseHandler;
    private HttpClient client;
    private ExecutorService executorService;

    private String contentTypePattern = "([a-z]+/[^;\\.]+);?\\s?(charset=)?(.*)";
    private Pattern pattern = Pattern.compile(contentTypePattern, Pattern.CASE_INSENSITIVE);

    public BuiltinRequester() {
        super(null);
    }

    @Override
    public void init() {
        try {
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
            sslContext.init(null, BuiltinTrustManager.get(), null);
//            executorService = Executors.newSingleThreadExecutor();
            client = HttpClient.newBuilder()
//                    .executor(executorService)
                    .connectTimeout(Duration.ofSeconds(6))
//                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .sslContext(sslContext)
                    .sslParameters(new SSLParameters())
                    //                .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 1080)))
//                                    .authenticator(Authenticator.getDefault())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("初始化请求器失败", e);
        }
    }

    @Override
    public CompletableFuture<BuiltinRequest> executeAsync(final BuiltinRequest request) {
        try {
            return client
                    .sendAsync(request.httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                    .whenCompleteAsync((httpResponse, throwable) -> {
                        request.lock();
                        try {
                            if (request.requestResult != null) {
                                return;
                            }
                            request.requestResult = new IRequest.RequestResult();
                            if (request.getStatus() == IRequest.Status.TIMEOUT) {
                                request.requestResult.setSucceed(false);
                                request.requestResult.setThrowable(new RuntimeException("该任务请求已超时，取消业务处理"));
                                return;
                            }
                            request.setStatus(IRequest.Status.RESPONSE);
                            if (httpResponse != null) {
                                handleSucceed(request, httpResponse);
                            } else if (throwable != null) {
                                handleThrowable(request, throwable);
                            } else {
                                log.error("出现了两参数均为空的异常现象!");
                                request.requestResult.setSucceed(false);
                                request.requestResult.setThrowable(new RuntimeException("出现了两参数均为空的异常现象"));
                            }
                            responseHandler.push(request);
                        } finally {
                            request.unlock();
                        }
                    })
                    .thenApplyAsync(httpResponse -> request)
                    ;
        } catch (Exception e) {
            log.error("内置请求器异常", e);
            request.requestResult = new BuiltinRequest.RequestResult();
            request.requestResult.setSucceed(false);
            request.requestResult.setThrowable(e);
            return CompletableFuture.completedFuture(request);
        }
    }

    @Override
    public BuiltinRequest create(String url) {
        return create(url, null);
    }

    @Override
    public BuiltinRequest create(String url, Object userData) {
        return new BuiltinRequest(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(6))
                        .build(),
                userData
        );
    }

    private void handleSucceed(BuiltinRequest request, HttpResponse<byte[]> httpResponse) {
        String contentType = null;
        try {
            request.httpRequest = httpResponse.request();
            request.httpResponse = httpResponse;
            String mimeType;
            Charset charset = null;
            contentType = httpResponse.headers().firstValue("content-type").orElse("");
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
                log.trace("无字符编码类型[{}] -> {}", contentType, httpResponse.request().uri());
                mimeType = "text/html";
            }
            byte[] body = httpResponse.body();
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
        } catch (Exception e) {
            log.error("框架解析响应失败 -> {}", contentType, e);
            request.requestResult.setSucceed(false);
            request.requestResult.setThrowable(e);
        } finally {
            // TODO 相关清理操作
        }
    }

    private void handleThrowable(BuiltinRequest request, Throwable throwable) {
        request.requestResult.setSucceed(false);
        request.requestResult.setThrowable(throwable.getCause());
    }

    @Override
    public void shutdown(boolean force) {
        if (null != executorService) {
            if (force)
                executorService.shutdownNow();
            else
                executorService.shutdown();
        }
        if (responseHandler != null) {
            responseHandler.shutdown(true);
        }
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private static class BuiltinTrustManager extends X509ExtendedTrustManager {
        static TrustManager[] get() {
            return new TrustManager[]{new BuiltinTrustManager()};
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
