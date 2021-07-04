package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.f;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface OptionalResult<T, E extends Throwable> extends BasicResult<T, E> {

    /**
     * Consumes instructions for what to do if a given result contains no value. This
     * method eliminates any uncertainty surrounding whether null values can exist within
     * the wrapper. As a result, its type will be upgraded to a more permissive
     * {@link PartialResult}.
     *
     * @param defaultGetter Supplies a default value if the wrapper does not contain one.
     * @return An upgraded {@link PartialResult} for handling unknown error types.
     */
    Result<T, E> defaultIfEmpty(final Supplier<T> defaultGetter);

    /**
     * Returns whether no value was returned in the process.
     *
     * e.g.
     * <pre>
     *   final OptionalResult<Object, RuntimeException> result = getOptionalResult();
     *   // Compute the result and proceed only if it returns null.
     *   if (result.isEmpty()) {
     *       ...
     *   }
     * </pre>
     *
     * @return true, if no value is present.
     */
    @CheckReturnValue
    boolean isEmpty();

    /**
     * Consumes a procedure to be executed if no value is present in the wrapper.
     *
     * @param f A function to run if this wrapper contains no value.
     * @return This, or else an upgraded {@link PartialResult}.
     */
    OptionalResult<T, E> ifEmpty(final Runnable f);

    /**
     * Returns whether the expected type of error occurred in the process.
     *
     * e.g.
     * <pre>
     *   final Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it errs.
     *   if (result.isErr()) {
     *       ...
     *   }
     * </pre>
     *
     * @return true, if an error is present.
     */
    @CheckReturnValue
    boolean isErr();

    /**
     * Accepts an expression for what to do in the event of an error being present.
     * <p>
     *   Use this whenever you want to functionally handle both code paths (i.e. error
     *   vs. value).
     * </p>
     * @throws WrongErrorException If the underlying error is an unexpected type.
     * @param f A function consuming the error, if present.
     * @return This, or else a complete {@link Result}.
     */
    OptionalResult<T, E> ifErr(final Consumer<E> f);

    /**
     * Returns whether a value was yielded or no error occurred.
     *
     * <p>e.g.</p>
     * <pre>
     *   final Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it does not err.
     *   if (result.isOk()) {
     *       ...
     *   }
     * <pre>
     *
     * @return true, if a value is present.
     */
    @CheckReturnValue
    boolean isOk();

    /**
     * Accepts an expression for what to do in the event of no error
     * being present. Use this whenever you want to functionally handle
     * both code paths (i.e. error vs. value).
     *
     * @param f A function consuming the value, if present.
     * @return This, or else a complete {@link Result}.
     */
    @CheckReturnValue
    OptionalResult<T, E> ifOk(Consumer<T> f);

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
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<T> get(final Consumer<E> func);

    /**
     * Effectively casts this object into a standard Optional instance.
     * Prefer calling {@link OptionalResult#get(Consumer)}, as this removes the need for
     * any implicit error checking.
     *
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<T> get();

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<E> getErr();

    /**
     * Yields the underlying value or else the input. This is equivalent to
     * running `getResult().get(e -> {...}).orElse();`
     *
     * @return The underlying value, or else the input.
     */
    @CheckReturnValue
    T orElse(T val);

    /**
     * Variant of {@link PartialResult#orElseGet(Function)} which does not specifically
     * consume the error.
     *
     * @param f A function which supplies a new value if none is present in the
     *             wrapper.
     * @return The underlying value, or else func.get().
     */
    @CheckReturnValue
    T orElseGet(final Supplier<T> f);

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
     * Attempts to retrieve the underlying error, asserting that one must exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @CheckReturnValue
    default E unwrapErr() {
        return expectErr("Attempted to unwrap a result with no error.");
    }

    /**
     * Asserts that no value or error will be present within the wrapper.
     *
     * @throws ResultUnwrapException If a value or an error is present.
     */
    default void assertEmpty() {
        this.expectEmpty("Wrapper contains a value or error.");
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
        return expect(f(message, args));
    }

    /**
     * Returns a generic type of {@link Throwable} which could be any exception.
     *
     * <p>
     *   You may wish to use this method instead of {@link Result#expectErr} as it is
     *   guaranteed to have a valid output and will never result in a {@link ClassCastException}.
     * </p>
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @return The underlying error.
     */
    E expectErr(final String message);

    /**
     * @see PartialOptionalResult#expectErr(String).
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying error.
     */
    default E expectErrF(final String message, final Object... args) {
        return this.expectErr(f(message, args));
    }

    /**
     * Asserts that no value or error will be present within the wrapper.
     *
     * @param message The message to display in the event of an error.
     * @throws ResultUnwrapException If a value or an error is present.
     */
    void expectEmpty(final String message);

    /**
     * Formatted variant of {@link #expectEmpty}.
     *
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @throws ResultUnwrapException If a value or an error is present.
     */
    default void expectEmptyF(final String message, final Object... args) {
        this.expectEmpty(f(message, args));
    }

    /**
     * Variant of {@link #unwrap} which throws the original error, if applicable.
     *
     * @throws E The original error, if present.
     * @return The underlying value.
     */
    default T orElseThrow() throws E {
        this.throwIfErr();
        return this.unwrap();
    }

    /**
     * If an error is present, it will be thrown by this method.
     *
     * @throws E The original error, if present.
     */
    void throwIfErr() throws E;
}
