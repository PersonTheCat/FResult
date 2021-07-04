package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.interfaces.ThrowingFunction;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static personthecat.fresult.Shorthand.f;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface PartialResult<T, E extends Throwable> extends BasicResult<T, E> {

    /**
     * Variant of {@link Result#isErr} which can safely guarantee whether the
     * expected type of error is present in the wrapper. If no error is present, this
     * function yields <code>false</code>. If the expected error is present, this function
     * yields <code>true</code>. If an unexpected error is found, <b>it will be thrown</b>.
     *
     * @throws WrongErrorException If any other type of exception is found.
     * @param clazz The type of error expected by the wrapper.
     * @return Whether an expected type of exception is found.
     */
    @CheckReturnValue
    boolean isErr(final Class<? super E> clazz);

    /**
     * Variant of {@link Result#isErr} which provides a semantic contract that the
     * type of error being handled is not significant.
     *
     * @return Whether <em>any</em> error is found in the wrapper.
     */
    @CheckReturnValue
    boolean isAnyErr();

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
    Result<T, E> ifErr(final Consumer<E> f);

    /**
     * Override providing a default behavior for all child types.
     *
     * @see PartialOptionalResult#get
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    default Optional<T> get(final Consumer<E> f) {
        return this.ifErr(f).get();
    }

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<Throwable> getAnyErr();

    /**
     * Converts this wrapper into a new type when accounting for both potential outcomes.
     *
     * @param ifOk The transformer applied if a value is present in the wrapper.
     * @param ifErr The transformer applied if an error is present in the wrapper.
     * @param <U> The type of value being output by this method.
     * @return The output of either <code>ifOk</code> or <code>ifErr</code>.
     */
    @CheckReturnValue
    <U> U fold(final Function<T, U> ifOk, final Function<E, U> ifErr);

    /**
     * Variant of {@link Result#get()} which simultaneously handles a potential error
     * and yields a substitute value. Equivalent to following a {@link Result#get()}
     * call with a call to {@link Optional#orElseGet}.
     *
     * @throws RuntimeException wrapping the original error, if an unexpected type
     *                          of error is present in the wrapper.
     * @param f A function which returns an alternate value given the error, if
     *             present.
     * @return The underlying value, or else func.apply(E).
     */
    @CheckReturnValue
    T orElseGet(final Function<E, T> f);

    /**
     * Maps to a new Result if an error is present. Use this whenever
     * your first and second attempt at retrieving a value may fail.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    PartialResult<T, E> orElseTry(final ThrowingFunction<E, T, E> f);

    /**
     * Attempts to retrieve the underlying value, asserting that one must exist.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    default T unwrap() {
        return this.expect("Attempted to unwrap a result with no value.");
    }

    /**
     * Attempts to retrieve the underlying error, asserting that one must exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @CheckReturnValue
    default Throwable unwrapErr() {
        return this.expectErr("Attempted to unwrap a result with no error.");
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
    Throwable expectErr(final String message);

    /**
     * @see PartialOptionalResult#expectErr(String).
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying error.
     */
    default Throwable expectErrF(final String message, final Object... args) {
        return expectErr(f(message, args));
    }

    /**
     * Variant of {@link #unwrap} which throws the original error, if applicable.
     *
     * @throws E The original error, if present.
     * @return The underlying value.
     */
    default T orElseThrow() throws Throwable {
        this.throwIfErr();
        return this.unwrap();
    }

    /**
     * If an error is present, it will be thrown by this method.
     *
     * @throws E The original error, if present.
     */
    void throwIfErr() throws Throwable;
}
