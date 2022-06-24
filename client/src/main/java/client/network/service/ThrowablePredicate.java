package client.network.service;

@FunctionalInterface
public interface ThrowablePredicate<T> {
    boolean test(T t) throws Exception;
}
