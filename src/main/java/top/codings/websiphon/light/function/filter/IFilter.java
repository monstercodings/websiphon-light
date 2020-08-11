package top.codings.websiphon.light.function.filter;

public interface IFilter<T, R> {
    R put(T t);

    R contain(T t);
}