package top.codings.websiphon.light.requester.support;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContextBuilder;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;

import javax.net.ssl.*;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BuiltinRequester extends CombineRequester implements AsyncRequester {
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
            executorService = Executors.newSingleThreadExecutor();
            client = HttpClient.newBuilder()
                    .executor(executorService)
                    .connectTimeout(Duration.ofSeconds(30))
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .sslContext(sslContext)
                    .sslParameters(new SSLParameters())
                    //                .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 1080)))
                    //                .authenticator(Authenticator.getDefault())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("初始化请求器失败", e);
        }
    }

    @Override
    public CompletableFuture<BuiltinRequest> executeAsync(BuiltinRequest request) {
        try {
            return client
                    .sendAsync(request.httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                    .whenCompleteAsync((httpResponse, throwable) -> request.requestResult = new BuiltinRequest.RequestResult())
                    .thenApplyAsync(httpResponse -> {
                        if (null == httpResponse) return null;
                        String contentType = null;
                        try {
                            request.httpRequest = httpResponse.request();
                            request.httpResponse = httpResponse;
                            String mimeType;
                            Charset charset;
                            contentType = httpResponse.headers().firstValue("content-type").orElse("");
                            Matcher matcher = pattern.matcher(contentType);
                            if (matcher.find()) {
                                mimeType = matcher.group(1);
                                if (StringUtils.isBlank(mimeType)) {
                                    mimeType = "text/html";
                                }
                                String charsetStr = matcher.group(3);
                                if (StringUtils.isBlank(charsetStr)) {
                                    charsetStr = "utf-8";
                                } else if (charsetStr.equals("binary")) {
                                    charsetStr = "utf-8";
                                }
                                if (charsetStr.contains(",")) {
                                    charsetStr = charsetStr.split(",")[0];
                                }
                                charset = Charset.forName(charsetStr);
                            } else {
                                log.trace("无字符编码类型[{}] -> {}", contentType, httpResponse.request().uri());
                                mimeType = "text/html";
                                charset = Charset.forName("utf-8");
                            }
                            if (mimeType.contains("text")) {
                                // 文本解析
                                request.requestResult.responseType = BuiltinRequest.ResponseType.TEXT;
                                request.requestResult.data = Optional.ofNullable(new String(httpResponse.body(), charset)).orElse("<html>该网页无内容</html>");
                            } else if (mimeType.contains("json")) {
                                // JSON解析
                                request.requestResult.responseType = BuiltinRequest.ResponseType.JSON;
                                request.requestResult.data = JSON.parse(Optional.ofNullable(new String(httpResponse.body(), charset)).orElse("{}"));
                            } else {
                                // 字节解析
                                request.requestResult.responseType = BuiltinRequest.ResponseType.UNKNOW;
                                request.requestResult.data = httpResponse.body();
                            }
                            return request;
                        } catch (Exception e) {
                            log.error("框架解析响应失败 -> {}", contentType, e);
                            request.requestResult.succeed = false;
                            request.requestResult.throwable = e;
                            return request;
                        } finally {
                            // TODO 相关清理操作
                        }
                    })
                    .exceptionallyAsync(throwable -> {
                        try {
                            request.requestResult.succeed = false;
                            request.requestResult.throwable = throwable.getCause();
                        } catch (Exception e) {
                            log.error("异常处理入队列失败", e);
                        }
                        return request;
                    })
                    .whenCompleteAsync((builtinRequest, throwable) -> responseHandler.push(request))
                    ;
        } catch (Exception e) {
            log.error("内置请求器异常", e);
            request.requestResult = new BuiltinRequest.RequestResult();
            request.requestResult.succeed = false;
            request.requestResult.throwable = e;
            return CompletableFuture.completedFuture(request);
        }
    }

    /*@Override
    public void error(BuiltinRequest request) {
        BuiltinRequest builtinRequest = request;
        for (Function<BuiltinRequest, BuiltinRequest> error : errors) {
            if ((builtinRequest = error.apply(builtinRequest)) == null) {
                break;
            }
        }
    }*/

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
        return true;
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
