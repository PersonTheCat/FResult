package personthecat.fresult.functions;

@FunctionalInterface
public interface ThrowingBiConsumer<T1, T2, E extends Throwable> {
    void accept(T1 t1, T2 t2) throws E;
}