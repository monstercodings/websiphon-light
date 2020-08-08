package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.light.requester.IRequest;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Getter
@Setter
public class BuiltinRequest implements IRequest<HttpRequest, HttpResponse<byte[]>> {
    HttpRequest httpRequest;
    HttpResponse<byte[]> httpResponse;
    Object userData;
    RequestResult requestResult;

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
