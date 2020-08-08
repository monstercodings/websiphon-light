package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import top.codings.websiphon.light.requester.IRequest;

@Setter
@Getter
public class ApacheRequest implements IRequest<HttpRequestBase, HttpResponse> {
    HttpRequestBase httpRequest;
    HttpResponse httpResponse;
    RequestResult requestResult;
    Object userData;

    public ApacheRequest(HttpRequestBase httpRequest) {
        this.httpRequest = httpRequest;
    }

    public ApacheRequest(HttpRequestBase httpRequest, Object userData) {
        this.httpRequest = httpRequest;
        this.userData = userData;
    }

    @Override
    public void release() {
        HttpClientUtils.closeQuietly(httpResponse);
        httpRequest = null;
        httpResponse = null;
        requestResult = null;
        userData = null;
    }
}
