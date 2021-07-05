package personthecat.fresult.functions;

import java.util.Optional;

@FunctionalInterface
public interface ThrowingOptionalBiFunction<T1, T2, R, E extends Throwable> {
    Optional<R> apply(T1 t1, T2 t2) throws E;
}
