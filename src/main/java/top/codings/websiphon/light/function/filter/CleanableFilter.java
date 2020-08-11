package top.codings.websiphon.light.function.filter;

public interface CleanableFilter<T, R> extends IFilter<T, R> {
    void clear();
}
