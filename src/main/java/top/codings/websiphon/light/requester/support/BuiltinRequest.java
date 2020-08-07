package top.codings.websiphon.light.requester.support;

import lombok.Getter;
import lombok.Setter;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Getter
public class BuiltinRequest {
    HttpRequest httpRequest;
    HttpResponse<byte[]> httpResponse;
    @Setter
    Object userData;
    RequestResult requestResult;

    public BuiltinRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public BuiltinRequest(HttpRequest httpRequest, Object data) {
        this.httpRequest = httpRequest;
        userData = data;
    }

    void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    void setHttpResponse(HttpResponse<byte[]> httpResponse) {
        this.httpResponse = httpResponse;
    }

    /**
     * 释放所持有的全部资源<br/>
     * 帮助GC快速回收
     */
    public void release() {
        httpRequest = null;
        httpResponse = null;
        userData = null;
        requestResult = null;
    }

    public static class RequestResult {
        @Getter
        boolean succeed = true;
        Throwable throwable;
        @Getter
        ResponseType responseType;
        Object data;

        public Throwable cause() {
            return throwable;
        }

        public Object get() {
            return data;
        }
    }

    public enum ResponseType {
        TEXT(),
        JSON(),
        UNKNOW(),
    }

}
