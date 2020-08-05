package top.codings.websiphon.light.function;

public interface IFilter<T, R> {
    R put(T t);

    R contain(T t);
}