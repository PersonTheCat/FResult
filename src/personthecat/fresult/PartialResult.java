package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.interfaces.ThrowingFunction;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static personthecat.fresult.Shorthand.f;

@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public abstract class PartialResult<T, E extends Throwable> {

    /**
     * Returns whether an error occurred in the process.
     *
     * e.g.
     * <pre><code>
     *   Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it errs.
     *   if (result.isErr()) {
     *       ...
     *   }
     * </code></pre>
     *
     * @return true, if an error is present.
     */
    @CheckReturnValue
    public abstract boolean isErr();

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
    public abstract Result<T, E> ifErr(Consumer<E> f);

    /**
     * Attempts to retrieve the underlying value, if present, while also accounting for
     * any potential errors.
     *
     * e.g.
     * <pre><code>
     *   Result.of(() -> getValueOrFail())
     *     .get(e -> {...})
     *     .ifPresent(t -> {...});
     * </code></pre>
     *
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    public Optional<T> get(Consumer<E> func) {
        return this.ifErr(func).get();
    }

    /**
     * Converts this wrapper into a new type when accounting for both potential outcomes.
     *
     * @param ifOk The transformer applied if a value is present in the wrapper.
     * @param ifErr The transformer applied if an error is present in the wrapper.
     * @param <U> The type of value being output by this method.
     * @return The output of either <code>ifOk</code> or <code>ifErr</code>.
     */
    @CheckReturnValue
    public abstract <U> U fold(Function<T, U> ifOk, Function<E, U> ifErr);

    /**
     * Attempts to retrieve the underlying value, asserting that one must exist.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    public T unwrap() {
        return expect("Attempted to unwrap a result with no value.");
    }

    /**
     * Attempts to retrieve the underlying error, asserting that one must exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @CheckReturnValue
    public E unwrapErr() {
        return expectErr("Attempted to unwrap a result with no error.");
    }

    /**
     * Yields the underlying value, throwing a convenient, generic exception, if an
     * error occurs.
     *
     * e.g.
     * <pre><code>
     *   // Runs an unsafe process, wrapping any original errors.
     *   Object result = getResult()
     *     .expect("Unable to get value from result.");
     * </code></pre>
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @param message The message to display in the event of an error.
     * @return The underlying value.
     */
    public abstract T expect(String message);

    /**
     * Formatted variant of {@link #expect}.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying value
     */
    public T expectF(String message, Object... args) {
        return expect(f(message, args));
    }

    /**
     * Yields the underlying error, throwing a generic exception if no error is present.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @return The underlying error.
     */
    @CheckReturnValue
    public abstract E expectErr(String message);

    /**
     * Formatted variant of {@link #expectErr}.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying error.
     */
    @CheckReturnValue
    public E expectErrF(String message, Object... args) {
        return expectErr(f(message, args));
    }

    /**
     * Variant of {@link #unwrap} which throws the original error, if applicable.
     *
     * @throws E The original error, if present.
     * @return The underlying value.
     */
    public T orElseThrow() throws E {
        this.throwIfPresent();
        return unwrap();
    }

    /**
     * If an error is present, it will be thrown by this method.
     *
     * @throws E The original error, if present.
     */
    public abstract void throwIfPresent() throws E;

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
    public abstract T orElseGet(Function<E, T> f);

    /**
     * Maps to a new Result if an error is present. Use this whenever
     * your first and second attempt at retrieving a value may fail.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    public abstract PartialResult<T, E> orElseTry(ThrowingFunction<E, T, E> f);
}
