package top.codings.websiphon.light.requester.support;

import io.netty.handler.codec.http.HttpRequest;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
public class NettyRequest extends BaseRequest<HttpRequest, NettyRequest.HttpResponse> {
    protected HttpRequest httpRequest;
    protected HttpResponse httpResponse;

    public NettyRequest(HttpRequest httpRequest) {
        this(httpRequest, null);
    }

    public NettyRequest(HttpRequest httpRequest, Object userData) {
        this.httpRequest = httpRequest;
        this.userData = userData;
    }

    @Override
    public void release() {
        super.release();
        httpRequest = null;
        httpResponse.uri = null;
        httpResponse = null;
    }

    @Getter
    @Setter
    public static class HttpResponse {
        private int code;
        private URI uri;

        public HttpResponse(URI uri) {
            this.uri = uri;
        }
    }
}
