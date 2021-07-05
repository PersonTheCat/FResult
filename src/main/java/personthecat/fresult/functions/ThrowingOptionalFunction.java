package personthecat.fresult.functions;

import java.util.Optional;

@FunctionalInterface
public interface ThrowingOptionalFunction<T, R, E extends Throwable> {
    Optional<R> apply(T val) throws E;
}
