package top.codings.websiphon.light.bean;

import lombok.Getter;

import java.util.function.Consumer;

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

    public TaskResult<T> then(Consumer<TaskResult<T>> consumer) {
        if (consumer != null) {
            consumer.accept(this);
        }
        return this;
    }
}
