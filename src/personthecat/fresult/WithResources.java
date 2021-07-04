package personthecat.fresult;

import personthecat.fresult.interfaces.ThrowingBiConsumer;
import personthecat.fresult.interfaces.ThrowingBiFunction;
import personthecat.fresult.interfaces.ThrowingConsumer;
import personthecat.fresult.interfaces.ThrowingFunction;
import personthecat.fresult.interfaces.ThrowingSupplier;

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
class WithResources<R1 extends AutoCloseable, R2 extends AutoCloseable, E extends Throwable> {

    private final ThrowingSupplier<R1, E> r1Getter;
    private final ThrowingSupplier<R2, E> r2Getter;

    WithResources(ThrowingSupplier<R1, E> r1Getter, ThrowingSupplier<R2, E> r2Getter) {
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
    public <T, E2 extends E> Result.Pending<T, E> of(ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.of(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which does not return a value.
     *
     * @param attempt A function which consumes both values and may throw an exception.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be OK or an error.
     */
    public <E2 extends E> Result.Pending<Void, E> of(ThrowingBiConsumer<R1, R2, E2> attempt) {
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
    public <T, E2 extends E> Result.Pending<T, E> of(ThrowingFunction<R2, T, E2> attempt) {
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
    public <E2 extends E> Result.Pending<Void, E> of(ThrowingConsumer<R2, E2> attempt) {
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
    public <T, E2 extends E> Result<T, Throwable> any(ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.any(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which implies that the type of exception
     * being thrown is not significant.
     *
     * @see WithResources#any(ThrowingBiFunction)
     * @param attempt A function which consumes both values and may throw an exception.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be OK or <b>any</b> error.
     */
    public <E2 extends E> Result<Void, Throwable> any(ThrowingBiConsumer<R1, R2, E2> attempt) {
        return this.any(Result.wrapVoid(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingFunction)} which implies that the type of exception
     * being thrown is not significant.
     *
     * @see WithResources#any(ThrowingBiFunction)
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or <b>any</b> error.
     */
    public <T, E2 extends E> Result<T, Throwable> any(ThrowingFunction<R2, T, E2> attempt) {
        return this.any((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link #of(ThrowingConsumer)} which implies that the type of exception
     * being thrown is not significant.
     *
     * @see WithResources#any(ThrowingBiFunction)
     * @param attempt A function which consumes the second value and may throw an exception.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either OK or an error.
     */
    public <E2 extends E> Result<Void, Throwable> any(ThrowingConsumer<R2, E2> attempt) {
        return this.any((r1, r2) -> { attempt.accept(r2); });
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
    public <T, E2 extends E> Result.PendingNullable<T, E> nullable(ThrowingBiFunction<R1, R2, T, E2> attempt) {
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
    public <T, E2 extends E> Result.PendingNullable<T, E> nullable(ThrowingFunction<R2, T, E2> attempt) {
        return this.nullable((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link WithResources#nullable(ThrowingBiFunction)} which wraps the given
     * value in Optional instead of returning an {@link PartialOptionalResult}. This may be useful
     * in some cases where it is syntactically shorter to handle null values via {@link Optional}.
     *
     * @param attempt A function which consumes both resources and either returns a value,
     *                throws an exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    public <T, E2 extends E> Result.Pending<Optional<T>, E> wrappingOptional(ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.wrappingOptional(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link WithResources#wrappingOptional(ThrowingBiFunction)} which wraps
     * the given value in Optional instead of returning an {@link PartialOptionalResult}.
     *
     * @see WithResources#wrappingOptional(ThrowingBiFunction)
     * @param attempt A function which consumes the first resource and either returns a
     *                value, throws an exception, or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E2> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    public <T, E2 extends E> Result.Pending<Optional<T>, E> wrappingOptional(ThrowingFunction<R2, T, E2> attempt) {
        return this.wrappingOptional((R1 r1, R2 r2) -> attempt.apply(r2));
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
    private <T, E2 extends E> T execute(ThrowingBiFunction<R1, R2, T, E2> attempt) throws E {
        try (R1 r1 = this.r1Getter.get(); R2 r2 = this.r2Getter.get()) {
            return attempt.apply(r1, r2);
        } catch (Throwable e) {
            throw Result.<E>errorFound(e);
        }
    }
}