package top.codings.websiphon.light.requester;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public interface IRequest<Q, R> {
    Q getHttpRequest();

    R getHttpResponse();

    void setHttpRequest(Q httpRequest);

    void setHttpResponse(R httpResponse);

    RequestResult getRequestResult();

    void setRequestResult(RequestResult requestResult);

    /**
     * 获取当前请求对象的状态
     * @return
     */
    Status getStatus();

    boolean setStatus(Status status);

    Object getUserData();

    void lock();

    boolean tryLock();

    void unlock();

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

    enum ResponseType {
        TEXT(),
        JSON(),
        BYTE(),
        NO_CHARSET(),
        ERROR_CODE(),
    }

    @AllArgsConstructor
    enum Status {
        WAIT("等待中"),
        READY("准备中"),
        REQUEST("请求中"),
        RESPONSE("处理响应中"),
        PROCESS("业务处理中"),
        FINISH("处理完成"),
        TIMEOUT("已超时"),
        ;
        public String text;
    }

}
