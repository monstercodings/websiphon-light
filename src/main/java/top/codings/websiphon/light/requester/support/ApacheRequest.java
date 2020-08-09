package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;

@Setter
@Getter
public class ApacheRequest extends BaseRequest<HttpRequestBase, HttpResponse> {
    protected HttpRequestBase httpRequest;
    protected HttpResponse httpResponse;

    public ApacheRequest(HttpRequestBase httpRequest) {
        this.httpRequest = httpRequest;
    }

    public ApacheRequest(HttpRequestBase httpRequest, Object userData) {
        this.httpRequest = httpRequest;
        this.userData = userData;
    }

    @Override
    public void release() {
        if (null != httpResponse) {
            HttpClientUtils.closeQuietly(httpResponse);
        }
        httpRequest = null;
        httpResponse = null;
        requestResult.setData(null);
        requestResult.setThrowable(null);
        requestResult = null;
        userData = null;
    }
}
