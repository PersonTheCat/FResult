package personthecat.fresult;

import personthecat.fresult.functions.*;

import java.util.Optional;

/**
 * A helper classed used for generating Results with two closeable resources.
 * Equivalent to using try-with-resources.
 *
 * @param <R1> The resource being consumed and closed by this handler.
 * @param <R2> The second resource being consumed and closed by this handler.
 * @param <E> The type of error to be caught by the handler.
 */
@SuppressWarnings("unused")
public class WithResources<R1 extends AutoCloseable, R2 extends AutoCloseable, E extends Throwable> {

    private final ThrowingSupplier<R1, E> r1Getter;
    private final ThrowingSupplier<R2, E> r2Getter;

    WithResources(final ThrowingSupplier<R1, E> r1Getter, final ThrowingSupplier<R2, E> r2Getter) {
        this.r1Getter = r1Getter;
        this.r2Getter = r2Getter;
    }

    /**
     * Constructs a new Result, consuming the resources and yielding a new value.
     * Equivalent to calling {@link Result#of} after queueing resources.
     *
     * <p>
     *   This ultimately changes the type of error handled by the wrapper, but the
     *   original error must be a subclass of the new error in order for it to work.
     * </p>
     *
     * @param attempt A function which consumes both resources and either returns a
     *                value or throws an exception.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or an error.
     */
    public <T, E2 extends E> Result.Pending<T, E> of(final ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.of(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which does not return a value.
     *
     * @param attempt A function which consumes both values and may throw an exception.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be OK or an error.
     */
    public <E2 extends E> Result.Pending<Void, E> of(final ThrowingBiConsumer<R1, R2, E2> attempt) {
        return this.of(Result.wrapVoid(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which only accepts the second resource
     * in the queue.
     *
     * @param attempt A function which consumes the second resource and either returns a
     *                value or throws an exception.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or an error.
     */
    public <T, E2 extends E> Result.Pending<T, E> of(final ThrowingFunction<R2, T, E2> attempt) {
        return this.of((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link #of(ThrowingBiConsumer)} which only accepts the second resource in
     * the queue.
     *
     * @param attempt A function which consumes the second value and may throw an exception.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either OK or an error.
     */
    public <E2 extends E> Result.Pending<Void, E> of(final ThrowingConsumer<R2, E2> attempt) {
        return this.of((r1, r2) -> { attempt.accept(r2); });
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which which implies that the type of
     * exception being thrown is not significant. For this reason, we ignore the safeguards
     * via type coercion and immediately return a {@link Result}.
     *
     * @param attempt A function which consumes both resources and either returns a value
     *                or throws <b>any</b> type of exception.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or an <b>any</b> error.
     */
    public <T, E2 extends E> Result<T, Throwable> suppress(final ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.suppress(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which implies that the type of exception
     * being thrown is not significant.
     *
     * @see WithResources#suppress(ThrowingBiFunction)
     * @param attempt A function which consumes both values and may throw an exception.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be OK or <b>any</b> error.
     */
    public <E2 extends E> Result<Void, Throwable> suppress(final ThrowingBiConsumer<R1, R2, E2> attempt) {
        return this.suppress(Result.wrapVoid(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingFunction)} which implies that the type of exception
     * being thrown is not significant.
     *
     * @see WithResources#suppress(ThrowingBiFunction)
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or <b>any</b> error.
     */
    public <T, E2 extends E> Result<T, Throwable> suppress(final ThrowingFunction<R2, T, E2> attempt) {
        return this.suppress((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link #of(ThrowingConsumer)} which implies that the type of exception
     * being thrown is not significant.
     *
     * @see WithResources#suppress(ThrowingBiFunction)
     * @param attempt A function which consumes the second value and may throw an exception.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either OK or an error.
     */
    public <E2 extends E> Result<Void, Throwable> suppress(final ThrowingConsumer<R2, E2> attempt) {
        return this.suppress((r1, r2) -> { attempt.accept(r2); });
    }

    /**
     * Variant of {@link WithResources#of(ThrowingBiFunction)} which is allowed to return
     * null.
     *
     * @param attempt A function which consumes both resources and either returns a value,
     *                throws an exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    public <T, E2 extends E> PartialOptionalResult<T, E> nullable(final ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.nullable(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link WithResources#of(ThrowingFunction)} which is allowed to return
     * null.
     *
     * @param attempt A function which consumes the first resource and either returns a
     *                value, throws an exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    public <T, E2 extends E> PartialOptionalResult<T, E> nullable(final ThrowingFunction<R2, T, E2> attempt) {
        return this.nullable((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link WithResources#nullable(ThrowingBiFunction)} which is allowed to
     * throw <b>any</b> kind of exception.
     *
     * @param attempt A function which consumes both resources and either returns a value,
     *                throws <b>any</b> exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, <b>any</b> error, or null.
     */
    public <T, E2 extends E> OptionalResult<T, Throwable> suppressNullable(final ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.suppressNullable(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link WithResources#nullable(ThrowingFunction)} which is allowed to
     * throw <b>any</b> kind of exception.
     *
     * @param attempt A function which consumes the first resource and either returns a
     *                value, throws <b>any</b> exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, <b>any</b> error, or null.
     */
    public <T, E2 extends E> OptionalResult<T, Throwable> suppressNullable(final ThrowingFunction<R2, T, E2> attempt) {
        return this.suppressNullable((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link WithResources#nullable(ThrowingBiFunction)} in which the return
     * value is wrapped in {@link Optional}.
     *
     * @param attempt A function which consumes both resources and either returns a value,
     *                throws an exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    public <T, E2 extends E> PartialOptionalResult<T, E> nullable(final ThrowingOptionalBiFunction<R1, R2, T, E2> attempt) {
        return this.nullable((ThrowingBiFunction<R1, R2, T, E>) (r1, r2) -> attempt.apply(r1, r2).orElse(null));
    }

    /**
     * Variant of {@link WithResources#nullable(ThrowingFunction)} in which the return
     * value is wrapped in {@link Optional}.
     *
     * @param attempt A function which consumes the first resource and either returns a
     *                value, throws an exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    public <T, E2 extends E> PartialOptionalResult<T, E> nullable(final ThrowingOptionalFunction<R2, T, E2> attempt) {
        return this.nullable((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link WithResources#suppressNullable(ThrowingBiFunction)} in which
     * the return value is wrapped in {@link Optional}.
     *
     * @param attempt A function which consumes both resources and either returns a value,
     *                throws <b>any</b> exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, <b>any</b> error, or null.
     */
    public <T, E2 extends E> OptionalResult<T, Throwable> suppressNullable(final ThrowingOptionalBiFunction<R1, R2, T, E2> attempt) {
        return this.suppressNullable((ThrowingBiFunction<R1, R2, T, E2>) (r1, r2) -> attempt.apply(r1, r2).orElse(null));
    }

    /**
     * Variant of {@link WithResources#suppressNullable(ThrowingFunction)} in which the
     * return value is wrapped in {@link Optional}.
     *
     * @param attempt A function which consumes the first resource and either returns a
     *                value, throws <b>any</b> exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, <b>any</b> error, or null.
     */
    public <T, E2 extends E> OptionalResult<T, Throwable> suppressNullable(final ThrowingOptionalFunction<R2, T, E2> attempt) {
        return this.suppressNullable((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Executes the underlying procedure being wrapped by this utility.
     *
     * @throws E If an error occurs.
     * @param attempt The procedure to attempt running which consumes the resource.
     * @param <T> The type of value being returned by the wrapper.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return The value expected by the wrapper.
     */
    private <T, E2 extends E> T execute(final ThrowingBiFunction<R1, R2, T, E2> attempt) throws E {
        try (R1 r1 = this.r1Getter.get(); R2 r2 = this.r2Getter.get()) {
            return attempt.apply(r1, r2);
        } catch (Throwable e) {
            throw Shorthand.<E>errorFound(e);
        }
    }
}