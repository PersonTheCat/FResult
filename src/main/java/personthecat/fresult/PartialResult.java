package personthecat.fresult;

import org.jetbrains.annotations.CheckReturnValue;
import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.functions.ThrowingFunction;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.f;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public sealed interface PartialResult<T, E extends Throwable>
        extends PartialOptionalResult<T, E>
        permits Result.Value, Result.Error, Result.Pending {

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
    @Override
    @CheckReturnValue
    boolean isErr(final Class<? super E> clazz);

    /**
     * Variant of {@link Result#isErr} which provides a semantic contract that the
     * type of error being handled is not significant.
     *
     * @return Whether <em>any</em> error is found in the wrapper.
     */
    @Override
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
    @Override
    Result<T, E> ifErr(final Consumer<E> f);

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    @Override
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
     * Strips away any uncertainty surrounding whether this wrapper contains an unexpected
     * exception or otherwise and returns a definite {@link Result.Value}.
     *
     * <p>e.g.</p>
     * <pre>
     *   final String value = Result.suppress(() -> "Hello, world!")
     *     .resolve(e -> "Default value")
     *     .expose(); // No consequences.
     * </pre>
     *
     * @param ifErr A handler which provides a default value.
     * @return A result which can only be a {@link Result.Value}.
     */
    @CheckReturnValue
    Result.Value<T, E> resolve(final Function<E, T> ifErr);

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
     * Maps to a new Result if an error is present. Use this whenever your first and
     * second attempt at retrieving a value may fail.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    PartialResult<T, E> orElseTry(final ThrowingFunction<E, T, E> f);

    /**
     * Variant of {@link #orElseTry(ThrowingFunction)} which handles the error when
     * given a specific {@link Protocol}.
     *
     * @param protocol Instructions for handling errors beyond this point.
     * @param f A new function to attempt in the presence of an error.
     * @return A result which may contain either a value or <b>any</b> error.
     */
    @CheckReturnValue
    Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingFunction<E, T, Throwable> f);

    /**
     * Variant of {@link #orElseTry(ThrowingFunction)} which handles the error when
     * given a {@link Resolver}.
     *
     * @param resolver Instructions for how to resolve a value in the presence of
     *                 an error.
     * @param f A new function to attempt in the presence of an error.
     * @return A result which can only be a value.
     */
    @CheckReturnValue
    Result.Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingFunction<E, T, Throwable> f);

    /**
     * Attempts to retrieve the underlying error, asserting that one must exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @Override
    @CheckReturnValue
    default Throwable unwrapErr() {
        return this.expectErr("Attempted to unwrap a result with no error.");
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
    @Override
    Throwable expectErr(final String message);

    /**
     * @see PartialOptionalResult#expectErr(String)
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying error.
     */
    @Override
    default Throwable expectErr(final String message, final Object... args) {
        return expectErr(f(message, args));
    }

    /**
     * Variant of {@link #unwrap} which throws the original error, if applicable.
     *
     * @throws E The original error, if present.
     * @return The underlying value.
     */
    @Override
    default T orElseThrow() throws Throwable {
        this.throwIfErr();
        return this.unwrap();
    }

    /**
     * If an error is present, it will be thrown by this method.
     *
     * @throws E The original error, if present.
     */
    @Override
    void throwIfErr() throws Throwable;

    /**
     * @deprecated Never empty.
     */
    @Override
    @Deprecated
    default PartialResult<T, E> defaultIfEmpty(final Supplier<T> defaultGetter) {
        return this;
    }

    /**
     * @deprecated Never empty.
     */
    @Override
    @Deprecated
    default PartialResult<T, E> ifEmpty(final Runnable f) {
        return this;
    }

    /**
     * @deprecated Never empty.
     */
    @Override
    @Deprecated
    default void assertEmpty() {}

    /**
     * @deprecated Never empty.
     */
    @Override
    @Deprecated
    default void expectEmpty(final String message) {}

    /**
     * @deprecated Never empty.
     */
    @Override
    @Deprecated
    default void expectEmpty(final String message, final Object... args) {}
}
