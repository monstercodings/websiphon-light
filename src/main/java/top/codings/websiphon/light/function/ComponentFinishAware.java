package top.codings.websiphon.light.function;

/**
 * 组件任务完成感知接口
 */
public interface ComponentFinishAware<T> {
    void finish(T t);
}
