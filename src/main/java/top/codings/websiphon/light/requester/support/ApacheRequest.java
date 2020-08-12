package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.Map;

@Setter
@Getter
public class ApacheRequest extends BaseRequest<HttpRequestBase> {
    protected HttpRequestBase httpRequest;

    public ApacheRequest(HttpRequestBase httpRequest) {
        this(httpRequest, null);
    }

    public ApacheRequest(HttpRequestBase httpRequest, Object userData) {
        this.httpRequest = httpRequest;
        this.userData = userData;
        uri = httpRequest.getURI();
    }

    @Override
    public void setHeaders(Map<String, Object> headers) {
        headers.forEach((s, o) -> httpRequest.setHeader(s, o.toString()));
    }

    @Override
    public void release() {
        super.release();
        httpRequest = null;
    }
}
