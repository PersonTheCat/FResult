package personthecat.fresult;

import personthecat.fresult.functions.ThrowingConsumer;
import personthecat.fresult.functions.ThrowingFunction;
import personthecat.fresult.functions.ThrowingSupplier;

import java.util.Optional;

/**
 * A helper classed used for generating Results with a single closeable resource.
 * Equivalent to using try-with-resources.
 *
 * @param <R> The resource being consumed and closed by this handler.
 * @param <E> The type of error to be caught by the handler.
 */
@SuppressWarnings("unused")
public class WithResource<R extends AutoCloseable, E extends Throwable> {

    private final ThrowingSupplier<R, E> rGetter;

    WithResource(final ThrowingSupplier<R, E> rGetter) {
        this.rGetter = rGetter;
    }

    /**
     * Constructs a new Result, consuming the resource and yielding a new value.
     * Equivalent to calling {@link Result#of} after queueing resources.
     * This ultimately changes the type of error handled by the wrapper, but the
     * original error must be a subclass of the new error in order for it to work.
     *
     * @param attempt A function which consumes the resource and either returns a
     *                value or throws an exception.
     * @param <T> The type of value being returned by the wrapper.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be a value or an error.
     */
    public <T, E2 extends E> Result.Pending<T, E> of(final ThrowingFunction<R, T, E2> attempt) {
        return Result.of(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingFunction)} which does not return a value.
     *
     * @param attempt A function which consumes the resource and may throw an
     *                exception.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be OK or an error.
     */
    public <E2 extends E> Result.Pending<Void, E> of(final ThrowingConsumer<R, E2> attempt) {
        return this.of(Result.wrapVoid(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingFunction)} which implies that the type of
     * exception being thrown is not significant. For this reason, we ignore
     * the safeguards via type coercion and immediately return a {@link Result}.
     *
     * @param attempt A function which consumes the resource and either returns a
     *                value or throws <b>any</b> type of exception.
     * @param <T> The type of value being returned by the wrapper.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be a value or <b>any</b> error.
     */
    public <T, E2 extends E> Result<T, Throwable> suppress(final ThrowingFunction<R, T, E2> attempt) {
        return Result.suppress(() -> this.execute(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingConsumer)} which implies that the type
     * of exception being thrown is not significant.
     *
     * @see WithResource#suppress(ThrowingFunction)
     * @param attempt A function which consumes the resource any either returns a
     *                value or throws <b>any</b> type of exception.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be a value or <b>any</b> error.
     */
    public <E2 extends E> Result<Void, Throwable> suppress(final ThrowingConsumer<R, E2> attempt) {
        return this.suppress(Result.wrapVoid(attempt));
    }

    /**
     * Variant of {@link WithResource#of} which is allowed to return a null value.
     *
     * @param attempt A function which consumes the resource and either returns a
     *                value, throws an exception, or returns null.
     * @param <T> The type of value being returned by the wrapper.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be a value, error, or null.
     */
    public <T, E2 extends E> PartialOptionalResult<T, E> nullable(final ThrowingFunction<R, T, E2> attempt) {
        return Result.nullable(() -> execute(attempt));
    }

    /**
     * Variant of {@link WithResource#nullable} which is allowed to throw <b>any</b>
     * kind of exception.
     *
     * @param attempt A function which consumes the resource and either returns a
     *                value, throws <b>any</b> exception, or returns null.
     * @param <T> The type of value being returned by the wrapper.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be a value, <b>any</b> error, or null.
     */
    public <T, E2 extends E> OptionalResult<T, Throwable> suppressNullable(final ThrowingFunction<R, T, E2> attempt) {
        return Result.suppressNullable(() -> execute(attempt));
    }

    /**
     * Variant of {@link WithResource#nullable) which wraps the given value in Optional
     * instead of returning an {@link PartialOptionalResult }. This may be useful in some
     * cases where it is syntactically shorter to handle null values via {@link Optional}.
     *
     * @param attempt A function which consumes the resource and either returns a
     *                value, throws an exception, or returns null.
     * @param <T> The type of value being returned by the wrapper.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be an optional value or an error.
     */
    public <T, E2 extends E> PartialResult<Optional<T>, E> wrappingOptional(final ThrowingFunction<R, T, E2> attempt) {
        return Result.wrappingOptional(() -> execute(attempt));
    }

    /**
     * Variant of {@link WithResource#wrappingOptional} which is allowed to throw <b>any</b>
     * kind of exception.
     *
     * @param attempt A function which consumes the resource and either returns a
     *                value, throws <b>any</b> exception, or returns null.
     * @param <T> The type of value being returned by the wrapper.
     * @param <E2> The type of exception thrown by the resource getter.
     * @return A result which may either be an optional value or <b>any</b> error.
     */
    public <T, E2 extends E> Result<Optional<T>, Throwable> suppressWrappingOptional(final ThrowingFunction<R, T, E2> attempt) {
        return Result.suppressWrappingOptional(() -> execute(attempt));
    }

    /**
     * Constructs a new handler with an additional resource.
     *
     * @param resource A function which either supplies the intended resource or
     *                 throws an exception.
     * @return A new wrapper containing getters for both resources.
     */
    public <R2 extends AutoCloseable> WithResources<R, R2, E> and(final ThrowingSupplier<R2, E> resource) {
        return new WithResources<>(rGetter, resource);
    }

    /**
     * Constructs a new handler with an additional resource yielded from the first.
     *
     * @param getter A function which accepts the first resource, yields the
     *               second, and may throw an exception.
     * @return A new wrapper containing getters for both resources.
     */
    public <R2 extends AutoCloseable> WithResources<R, R2, E> and(final ThrowingFunction<R, R2, E> getter) {
        return new WithResources<>(rGetter, () -> getter.apply(rGetter.get()));
    }

    /**
     * Optional syntactic variant of {@link #and(ThrowingSupplier)} with equivalent
     * functionality.
     *
     * @param resource A function which either supplies the intended resource or
     *                 throws an exception.
     * @return A new wrapper containing getters for both resources.
     */
    public <R2 extends AutoCloseable> WithResources<R, R2, E> with(final ThrowingSupplier<R2, E> resource) {
        return this.and(resource);
    }

    /**
     * Optional syntactic variant of {@link #and(ThrowingFunction)} with equivalent
     * functionality.
     *
     * @param getter A function which accepts the first resource, yields the
     *               second, and may throw an exception.
     * @return A new wrapper containing getters for both resources.
     */
    public <R2 extends AutoCloseable> WithResources<R, R2, E> with(final ThrowingFunction<R, R2, E> getter) {
        return this.and(getter);
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
    private <T, E2 extends E> T execute(final ThrowingFunction<R, T, E2> attempt) throws E {
        try (R r = this.rGetter.get()) {
            return attempt.apply(r);
        } catch (Throwable e) {
            throw Shorthand.<E>errorFound(e);
        }
    }
}