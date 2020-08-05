package top.codings.websiphon.light.function;

public interface CleanableFilter<T, R> extends IFilter<T, R> {
    void clear();
}
