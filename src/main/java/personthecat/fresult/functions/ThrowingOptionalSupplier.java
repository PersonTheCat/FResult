package personthecat.fresult.functions;

import java.util.Optional;

@FunctionalInterface
public interface ThrowingOptionalSupplier<T, E extends Throwable> {
    Optional<T> get() throws E;
}
