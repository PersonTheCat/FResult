package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.function.Consumer;

import static personthecat.fresult.Shorthand.f;

/**
 * A common parent interface for all result types. The final hierarchy is WIP.
 */
@SuppressWarnings("unused")
public interface BasicResult<T, E extends Throwable> {

    /**
     * Attempts to retrieve the underlying value, if present, while also accounting for
     * any potential errors.
     *
     * e.g.
     * <pre>
     *   Result.of(() -> getValueOrFail())
     *     .get(e -> {...})
     *     .ifPresent(t -> {...});
     * </pre>
     *
     * @param f A handler for any errors that arise.
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<T> get(final Consumer<E> f);

    /**
     * Attempts to retrieve the underlying value, asserting that one must exist.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    default T unwrap() {
        return expect("Attempted to unwrap a result with no value.");
    }

    /**
     * Yields the underlying value, throwing a convenient, generic exception, if an
     * error occurs.
     *
     * e.g.
     * <pre>
     *   // Runs an unsafe process, wrapping any original errors.
     *   Object result = getResult()
     *     .expect("Unable to get value from result.");
     * </pre>
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @param message The message to display in the event of an error.
     * @return The underlying value.
     */
    T expect(final String message);

    /**
     * Formatted variant of {@link #expect}.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying value
     */
    default T expectF(final String message, final Object... args) {
        return this.expect(f(message, args));
    }
}
