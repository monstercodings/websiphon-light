package top.codings.websiphon.light.bean;

import lombok.Getter;

public class TaskResult<T> {
    @Getter
    private boolean succeed;
    private Throwable throwable;
    @Getter
    private T data;

    public Throwable cause() {
        return throwable;
    }

    public TaskResult(boolean succeed, Throwable throwable, T data) {
        this.succeed = succeed;
        this.throwable = throwable;
        this.data = data;
    }
}
