package top.codings.websiphon.light.requester;

import lombok.Getter;
import lombok.Setter;

public interface IRequest<Q, R> {
    Q getHttpRequest();

    R getHttpResponse();

    void setHttpRequest(Q httpRequest);

    void setHttpResponse(R httpResponse);

    RequestResult getRequestResult();

    void setRequestResult(RequestResult requestResult);

    Object getUserData();

    /**
     * 释放所持有的全部资源<br/>
     * 帮助GC快速回收
     */
    default void release() {

    }

    @Setter
    class RequestResult {
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
