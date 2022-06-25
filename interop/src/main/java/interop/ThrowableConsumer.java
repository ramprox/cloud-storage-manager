package interop;

@FunctionalInterface
public interface ThrowableConsumer<T> {
    void accept(T message) throws Exception;
}
