package personthecat.fresult;

import java.util.Optional;

import static personthecat.fresult.Shorthand.*;

/** An object designed to hold either a value or error. */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class Value<T, E extends Throwable> {
    final Optional<T> result;
    final Optional<E> err;

    Value(Optional<T> result, Optional<E> err) {
        this.result = result;
        this.err = err;
    }

    static <T, E extends Throwable> Value<T, E> ok(T val) {
        return new Value<>(full(val), empty());
    }

    static <T, E extends Throwable> Value<T, E> err(E e) {
        return new Value<>(empty(), full(e));
    }

    /**
     * A generic Value with no result or error. The type parameters would be
     * lost at runtime and thus do not matter anyway.
     */
    static <E extends Throwable> Value<Void, E> ok() {
        return ok(Void.INSTANCE);
    }
}