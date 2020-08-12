package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

@Getter
@Setter
public class BuiltinRequest extends BaseRequest<HttpRequest> {
    protected HttpRequest httpRequest;

    public BuiltinRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
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
        headers.forEach((k, v) -> {
            if (
                    k.equalsIgnoreCase("Connection") ||
                            k.equalsIgnoreCase("Upgrade-Insecure-Requests") ||
                            k.equalsIgnoreCase("Host")
            ) {
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
        requestResult = null;
    }
}
