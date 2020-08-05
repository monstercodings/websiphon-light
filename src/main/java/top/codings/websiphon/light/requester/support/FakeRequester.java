package top.codings.websiphon.light.requester.support;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.light.manager.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class FakeRequester extends CombineRequester implements AsyncRequester {
    private Map<String, String> builtHeaders;

    public FakeRequester(CombineRequester requester) {
        this(requester, null);
    }

    public FakeRequester(CombineRequester requester, Map<String, String> builtHeaders) {
        super(requester);
        if (!(requester instanceof AsyncRequester)) {
            throw new RuntimeException("伪装头代理请求器必须基于异步请求器");
        }
        if (null == builtHeaders) {
            builtHeaders = new HashMap<>();
            builtHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            builtHeaders.put("Accept-Language", "zh-CN,zh;q=0.9");
            builtHeaders.put("Cache-Control", "no-cache");
//            builtHeaders.put("Connection", "keep-alive");
            builtHeaders.put("DNT", "1");
//            builtHeaders.put("Upgrade-Insecure-Requests", "1");
            builtHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
        }
        this.builtHeaders = builtHeaders;
    }

    @Override
    public CompletableFuture<BuiltinRequest> executeAsync(BuiltinRequest request) {
        try {
            HttpRequest httpRequest = request.httpRequest;
            Map<String, List<String>> headers = request.httpRequest.headers().map();
            if (headers == null || headers.isEmpty()) {
                HttpRequest.Builder builder = HttpRequest
                        .newBuilder()
                        .uri(httpRequest.uri())
                        .method(httpRequest.method(), httpRequest.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                        .version(httpRequest.version().orElse(HttpClient.Version.HTTP_1_1))
                        .timeout(httpRequest.timeout().orElse(Duration.ofSeconds(30)))
                        .expectContinue(httpRequest.expectContinue());
                builtHeaders.forEach((k, v) -> builder.header(k, v));
                request.httpRequest = builder.build();
            }
            return requester.executeAsync(request);
        } catch (Exception e) {
            log.error("伪装请求头失败", e);
            request.requestResult = new BuiltinRequest.RequestResult();
            request.requestResult.succeed = false;
            request.requestResult.throwable = e;
            return CompletableFuture.completedFuture(request);
        }
    }

    @Override
    public void setResponseHandler(QueueResponseHandler responseHandler) {
        ((AsyncRequester) requester).setResponseHandler(responseHandler);
    }
}
