package top.codings.websiphon.light.requester.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import top.codings.websiphon.light.function.handler.QueueResponseHandler;
import top.codings.websiphon.light.requester.AsyncRequester;
import top.codings.websiphon.light.requester.IRequest;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class FakeRequester extends CombineRequester<IRequest> implements AsyncRequester<IRequest> {
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
    public CompletableFuture<IRequest> executeAsync(IRequest request) {
        if (request instanceof BuiltinRequest) {
            HttpRequest httpRequest = (HttpRequest) request.getHttpRequest();
            Map<String, List<String>> headers = httpRequest.headers().map();
            if (headers == null || headers.isEmpty()) {
                HttpRequest.Builder builder = HttpRequest
                        .newBuilder()
                        .uri(httpRequest.uri())
                        .method(httpRequest.method(), httpRequest.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                        .version(httpRequest.version().orElse(HttpClient.Version.HTTP_2))
                        .timeout(httpRequest.timeout().orElse(Duration.ofSeconds(6)))
                        .expectContinue(httpRequest.expectContinue());
                builtHeaders.forEach((k, v) -> builder.header(k, v));
                request.setHttpRequest(builder.build());
            }
            /*try {
            } catch (Exception e) {
                log.error("伪装请求头失败", e);
                IRequest.RequestResult requestResult = new IRequest.RequestResult();
                requestResult.setSucceed(false);
                requestResult.setThrowable(e);
                request.setRequestResult(requestResult);
            }*/
        } else if (request instanceof ApacheRequest) {
            HttpRequestBase httpRequestBase = ((ApacheRequest) request).getHttpRequest();
            if (httpRequestBase.getAllHeaders().length == 0) {
                httpRequestBase.setHeaders(new Header[]{
                        new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"),
                        new BasicHeader("Accept-Language", "zh-CN,zh;q=0.9"),
                        new BasicHeader("Cache-Control", "no-cache"),
                        new BasicHeader("Connection", "keep-alive"),
                        new BasicHeader("Pragma", "no-cache"),
                        new BasicHeader("DNT", "1"),
                        new BasicHeader("Upgrade-Insecure-Requests", "1"),
                        new BasicHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36")
                });
            }
        } else if (request instanceof NettyRequest) {
            io.netty.handler.codec.http.HttpRequest httpRequest = ((NettyRequest) request).getHttpRequest();
            /*for (Map.Entry<String, String> entry : builtHeaders.entrySet()) {
                httpRequest.headers().set(entry.getKey(), entry.getValue());
            }*/
            httpRequest.headers()
                    .set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .set("Accept-Encoding", "gzip, deflate, compress")
                    .set("Accept-Language", "zh-CN,zh;q=0.9")
                    .set("Cache-Control", "no-cache")
                    .set("Connection", "keep-alive")
                    .set("DNT", "1")
                    .set("Upgrade-Insecure-Requests", "1")
                    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36")
            ;
        } else {
            return CompletableFuture.completedFuture(request);
        }
        return requester.executeAsync(request);
    }

    @Override
    public void setResponseHandler(QueueResponseHandler responseHandler) {
        ((AsyncRequester) requester).setResponseHandler(responseHandler);
    }
}
