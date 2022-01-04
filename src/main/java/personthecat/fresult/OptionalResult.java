package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.functions.OptionalResultFunction;
import personthecat.fresult.functions.ThrowingFunction;
import personthecat.fresult.functions.ThrowingSupplier;

import javax.annotation.CheckReturnValue;
import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.f;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface OptionalResult<T, E extends Throwable> extends BasicResult<T, E>, Serializable {

    /**
     * Consumes instructions for what to do if a given result contains no value. This
     * method eliminates any uncertainty surrounding whether null values can exist within
     * the wrapper. As a result, its type will be upgraded to a more permissive
     * {@link Result}.
     *
     * @param defaultGetter Supplies a default value if the wrapper does not contain one.
     * @return An upgraded {@link Result} for handling known error types.
     */
    Result<T, E> defaultIfEmpty(final Supplier<T> defaultGetter);

    /**
     * Generates an error in the case where no value is present at all in this wrapper.
     *
     * <p>As with {@link #defaultIfEmpty(Supplier)}, this method eliminates the possibility
     * of empty state, thus allowing it to return a standard {@link Result}.
     *
     * @param defaultGetter Supplies an error if the wrapper contains no value.
     * @return An upgraded {@link Result} for handling known error types.
     */
    Result<T, E> errIfEmpty(final Supplier<E> defaultGetter);

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
    OptionalResult<T, E> ifOk(Consumer<T> f);

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
     * Variant of {@link Result#orElseTry(ThrowingFunction)} which ignores any errors present
     * in the wrapper.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    PartialResult<T, E> orElseTry(final ThrowingSupplier<T, E> f);

    /**
     * Variant of {@link Result#orElseTry(ThrowingFunction)} which handles the error when
     * given a specific {@link Protocol}.
     *
     * @param protocol Instructions for handling errors beyond this point.
     * @param f A new function to attempt in the presence of an error.
     * @return A result which may contain either a value or <b>any</b> error.
     */
    @CheckReturnValue
    Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingSupplier<T, Throwable> f);

    /**
     * Replaces the underlying value with the result of func.apply(). Use this
     * whenever you need to map a potential value to a different value.
     *
     * <p>e.g.</p>
     * <pre>
     *   final int secretArrayLength = Result.of(() -> getSecretArray())
     *      .get(e -> {...}) // Handle any errors.
     *      .map(array -> array.length) // Return the length instead.
     *      .orElse(0); // Didn't work. Call Optional#orElse().
     * </pre>
     *
     * @param f The mapper applied to the value.
     * @return A new Result with its value mapped.
     */
    @CheckReturnValue
    <M> OptionalResult<M, E> map(final Function<T, M> f);

    /**
     * Replaces the underlying error with the result of func.apply(). Use this
     * whenever you need to map a potential error to another error.
     *
     * @param f The mapper applied to the error.
     * @return A new Result with its error mapped.
     */
    @CheckReturnValue
    <E2 extends Throwable> OptionalResult<T, E2> mapErr(final Function<E, E2> f);

    /**
     * Replaces the entire value with a new result, if present. Use this whenever
     * you need to map a potential value to another function which yields a Result.
     *
     * @param f A function which yields a new Result wrapper if a value is present.
     * @return The new function yielded, if a value is present, else this.
     */
    @CheckReturnValue
    <M> OptionalResult<M, E> flatMap(final OptionalResultFunction<T, M, E> f);

    /**
     * Replaces the entire value with a new result, if present. Use this whenever
     * you need to map a potential error to another function which yields a Result.
     *
     * @param f A function which yields a new Result wrapper if an error is present.
     * @return The new function yielded, if an error is present, or else a complete
     *         {@link Result}.
     */
    @CheckReturnValue
    <E2 extends Throwable> OptionalResult<T, E2> flatMapErr(final OptionalResultFunction<E, T, E2> f);

    /**
     * Filters the underlying value out of the wrapper based on the given condition.
     *
     * <p>e.g.</p>
     * <pre>
     *   Result.of(() -> "Hello, world!) // Value is present
     *     .filter(() -> false) // Value is removed.
     *     .assertEmpty(); // No exception is thrown.
     * </pre>
     *
     * @param f A predicate which determines whether to keep the underlying value, if present.
     * @return A new result with no value, or else this.
     */
    @CheckReturnValue
    OptionalResult<T, E> filter(final Predicate<T> f);

    /**
     * Filters the underlying error out of the wrapper based on the given condition.
     *
     * <p>e.g.</p>
     * <pre>
     *   Result.of(() -> throwException()) // Error is present
     *     .filterErr(() -> false) // Error is removed.
     *     .assertEmpty(); // No exception is thrown.
     * </pre>
     *
     * @param f A predicate which determines whether to keep the underlying error, if present.
     * @return A new result with no error, or else this.
     */
    @CheckReturnValue
    OptionalResult<T, E> filterErr(final Predicate<E> f);

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
