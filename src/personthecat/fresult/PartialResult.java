package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.interfaces.ThrowingFunction;

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
    public abstract boolean isErr();

    /**
     * Accepts an expression for what to do in the event of an error
     * being present. Use this whenever you want to functionally handle
     * both code paths (i.e. error vs. value).
     *
     * @param f A function consuming the error, if present.
     * @return This, or else a complete {@link Result}.
     */
    public abstract Result<T, E> ifErr(Consumer<E> f);

    /**
     * Returns whether a value was yielded or no error occurred.
     *
     * e.g.
     * <pre><code>
     *   Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it does not err.
     *   if (result.isOk()) {
     *       ...
     *   }
     * </code></pre>
     *
     * @return true, if a value is present.
     */
    public abstract boolean isOk();

    /**
     * Accepts an expression for what to do in the event of no error
     * being present. Use this whenever you want to functionally handle
     * both code paths (i.e. error vs. value).
     *
     * @param f A function consuming the value, if present.
     * @return This, or else a complete {@link Result}.
     */
    public abstract Result<T, E> ifOk(Consumer<T> f);

    /**
     * Accepts an expression for handling the expected error, if present.
     * Use this whenever you want to get the error, but still have
     * access to the Result wrapper.
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
    public Optional<T> get(Consumer<E> func) {
        return this.ifErr(func).get();
    }

    /**
     * Attempts to retrieve the underlying value, asserting that one must
     * exist.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    public T unwrap() {
        return expect("Attempted to unwrap a result with no value.");
    }

    /**
     * Attempts to retrieve the underlying error, asserting that one must
     * exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    public E unwrapErr() {
        return expectErr("Attempted to unwrap a result with no error.");
    }

    /**
     * Yields the underlying value, throwing a convenient, generic exception,
     * if an error occurs.
     *
     * e.g.
     * <pre><code>
     *   // Runs an unsafe process, wrapping any original errors.
     *   Object result = getResult()
     *     .expect("Unable to get value from result.");
     * </code></pre>
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    public abstract T expect(String message);

    /** Formatted variant of {@link #expect}. */
    public T expectF(String message, Object... args) {
        return expect(f(message, args));
    }

    /**
     * Yields the underlying error, throwing a generic exception if no error
     * is present.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    public abstract E expectErr(String message);

    /** Formatted variant of {@link #expectErr}. */
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
    public abstract T orElseGet(Function<E, T> f);

    /**
     * Maps to a new Result if an error is present. Use this whenever
     * your first and second attempt at retrieving a value may fail.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    public abstract PartialResult<T, E> orElseTry(ThrowingFunction<E, T, E> f);
}
