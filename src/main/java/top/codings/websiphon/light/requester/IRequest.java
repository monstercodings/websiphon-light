package top.codings.websiphon.light.requester;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.entity.ContentType;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;

public interface IRequest<T> {
    T getHttpRequest();

    void setHttpRequest(T httpRequest);

    RequestResult getRequestResult();

    void setRequestResult(RequestResult requestResult);

    URI getUri();

    void setHeaders(Map<String, Object> headers);

    /**
     * 获取当前请求对象的状态
     *
     * @return
     */
    Status getStatus();

    boolean setStatus(Status status);

    Object getUserData();

    void setUserData(Object userData);

    ContentType getContentType();

    void setContentType(ContentType contentType);

    void setProxy(Proxy proxy);

    Proxy getProxy();

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
        @Getter
        int code = -1;
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
        ERROR("响应异常"),
        ;
        public String text;
    }

}
