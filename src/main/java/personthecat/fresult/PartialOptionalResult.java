package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.f;

/**
 * The parent type of all possible {@link Result} values.
 *
 * <p>
 *   The <code>OptionalResult</code> type represents the least amount of certainty possible
 *   for any given outcome. It implies that a procedure has terminated in one of three
 *   potential states:
 * </p>
 * <ul>
 *   <li>A value,</li>
 *   <li>An error, or</li>
 *   <li><code>null</code>.</li>
 * </ul>
 * <p>
 *   As a result, this type provides the smallest set of functional utilities for interacting
 *   with the data. To achieve a higher level of certainty about the state of this wrapper,
 *   you can begin by stripping away the layers uncertainty, starting in most cases with
 *   nullability.
 *
 *   In general, nullability is the first layer to be stripped away. This can be accomplished
 *   by supplying a default value via {@link PartialOptionalResult#defaultIfEmpty}, which in turn
 *   yields a {@link PartialResult}.
 * </p>
 * <pre>
 *   final PartialResult<String, RuntimeException> result = Result.nullable(() -> null)
 *     .defaultIfEmpty(() -> "Hello, World!");
 * </pre>
 * <p>
 *   Alternatively, you may wish to handle this scenario by some other means. This can be
 *   accomplished by calling {@link PartialOptionalResult#ifEmpty}.
 * </p>
 * <pre>
 *   final OptionalResult<String, RuntimeException> result = Result.nullable(() -> null)
 *     .ifEmpty(() -> {...});
 * </pre>
 * <p>
 *   Finally, FResult also provides utilities for terminating early if an error or null is
 *   present ({@link #expect}, {@link #unwrap}, etc.). Let's take a look at the quickest way
 *   to stop early if anything goes wrong:
 * </p>
 * <pre>
 *   final String result = Result.<String, RuntimeException>nullable(() -> null)
 *     .expect("Error message!");
 * </pre>
 *
 * @see PartialResult
 * @param <T> The type of data being consumed by the wrapper.
 * @param <E> The type of error being consumed by the wrapper.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface PartialOptionalResult<T, E extends Throwable> extends BasicResult<T, E> {

    /**
     * Consumes instructions for what to do if a given result contains no value. This
     * method eliminates any uncertainty surrounding whether null values can exist within
     * the wrapper. As a result, its type will be upgraded to a more permissive
     * {@link PartialResult}.
     *
     * @param defaultGetter Supplies a default value if the wrapper does not contain one.
     * @return An upgraded {@link PartialResult} for handling unknown error types.
     */
    PartialResult<T, E> defaultIfEmpty(final Supplier<T> defaultGetter);

    /**
     * Generates an error in the case where no value is present at all in this wrapper.
     *
     * <p>As with {@link #defaultIfEmpty(Supplier)}, this method eliminates the possibility
     * of empty state, thus allowing it to return a standard, {@link PartialResult partial
     * result}.
     *
     * @param defaultGetter Supplies an error if the wrapper contains no value.
     * @return An upgraded {@link PartialResult} for handling unknown error types.
     */
    PartialResult<T, E> errIfEmpty(final Supplier<E> defaultGetter);

    /**
     * Consumes a procedure to be executed if no value is present in the wrapper.
     *
     * @param f A function to run if this wrapper contains no value.
     * @return This, or else an upgraded {@link PartialResult}.
     */
    PartialOptionalResult<T, E> ifEmpty(final Runnable f);

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
    OptionalResult<T, E> ifErr(final Consumer<E> f);

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<Throwable> getAnyErr();

    /**
     * Attempts to retrieve the underlying error, asserting that one must exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @CheckReturnValue
    default Throwable unwrapErr() {
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
    default void expectEmpty(final String message, final Object... args) {
        this.expectEmpty(f(message, args));
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
    default Throwable expectErr(final String message, final Object... args) {
        return this.expectErr(f(message, args));
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
