package top.codings.websiphon.light.function;

public interface ComponentInitAware<T> {
    void init(T t) throws Exception;
}
