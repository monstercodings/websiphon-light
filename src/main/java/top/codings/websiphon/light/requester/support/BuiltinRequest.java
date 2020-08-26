package top.codings.websiphon.light.requester.support;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Getter
@Setter
public class BuiltinRequest extends BaseRequest<HttpRequest> {
    protected HttpRequest httpRequest;

    public BuiltinRequest(HttpRequest httpRequest) {
        this(httpRequest, null);
    }

    public BuiltinRequest(HttpRequest httpRequest, Object data) {
        this.httpRequest = httpRequest;
        userData = data;
        uri = httpRequest.uri();
    }

    @Override
    public void setHeaders(Map<String, Object> headers) {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder()
                .uri(httpRequest.uri())
                .method(httpRequest.method(), httpRequest.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                .version(httpRequest.version().orElse(HttpClient.Version.HTTP_2))
                .timeout(httpRequest.timeout().orElse(Duration.ofSeconds(6)))
                .expectContinue(httpRequest.expectContinue());
        Map<String, String> headerMap = new ConcurrentHashMap<>();
        httpRequest.headers().map().entrySet().parallelStream()
        .forEach(stringListEntry -> {
            String key = stringListEntry.getKey();
            String value = stringListEntry.getValue().parallelStream()
                    .reduce((s, s2) -> String.join(";")).get();
            headerMap.put(key, value);
            builder.header(key, value);
        });
        headers.forEach((k, v) -> {
            if (headerMap.containsKey(k)) {
                return;
            }
            if (
                    k.equalsIgnoreCase("Connection") ||
                            k.equalsIgnoreCase("Upgrade-Insecure-Requests") ||
                            k.equalsIgnoreCase("Host")
            ) {
                return;
            }
            if (k.equalsIgnoreCase("Accept-Encoding")) {
                return;
            }
            builder.header(k, v.toString());
        });
        httpRequest = builder.build();
    }

    /**
     * 释放所持有的全部资源<br/>
     * 帮助GC快速回收
     */
    @Override
    public void release() {
        super.release();
        httpRequest = null;
        userData = null;
    }
}
