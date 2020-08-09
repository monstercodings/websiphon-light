package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Getter
@Setter
public class BuiltinRequest extends BaseRequest<HttpRequest, HttpResponse<byte[]>> {
    protected HttpRequest httpRequest;
    protected HttpResponse<byte[]> httpResponse;

    public BuiltinRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public BuiltinRequest(HttpRequest httpRequest, Object data) {
        this.httpRequest = httpRequest;
        userData = data;
    }

    /**
     * 释放所持有的全部资源<br/>
     * 帮助GC快速回收
     */
    @Override
    public void release() {
        httpRequest = null;
        httpResponse = null;
        userData = null;
        requestResult = null;
    }
}
