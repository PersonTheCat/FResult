package personthecat.fresult;

import personthecat.fresult.interfaces.*;

/**
 * A helper classed used for generating Results with a single closeable resource.
 * Equivalent to using try-with-resources.
 *
 * @param <R> The resource being consumed and closed by this handler.
 * @param <E> The type of error to be caught by the handler.
 */
public class WithResource<R extends AutoCloseable, E extends Throwable> {
    private final ThrowingSupplier<R, E> rGetter;

    WithResource(ThrowingSupplier<R, E> rGetter) {
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
     * @return The result of the operation.
     */
    public <T, E2 extends Throwable> Result<T, E2> of(ThrowingFunction<R, T, E2> attempt) {
        return new Result<>(() -> getValue(attempt));
    }

    /**
     * Variant of {@link #of(ThrowingFunction)} which does not return a value.
     *
     * @param attempt A function which consumes the resource and may throw an
     *                exception.
     * @return The result of the operation.
     */
    public <E2 extends Throwable> Result<Void, E2> of(ThrowingConsumer<R, E2> attempt) {
        return of(r -> {
            attempt.accept(r);
            return Void.INSTANCE;
        });
    }

    /**
     * Constructs a new handler with an additional resource.
     *
     * @param resource A function which either supplies the intended resource or
     *                 throws an exception.
     * @return A new wrapper containing getters for both resources.
     */
    public <R2 extends AutoCloseable> WithResources<R, R2, E> with(ThrowingSupplier<R2, E> resource) {
        return new WithResources<>(rGetter, resource);
    }

    /**
     * Constructs a new handler with an additional resource yielded from the first.
     *
     * @param getter A function which accepts the first resource, yields the
     *               second, and may throw an exception.
     * @return A new wrapper containing getters for both resources.
     */
    public <R2 extends AutoCloseable> WithResources<R, R2, E> with(ThrowingFunction<R, R2, E> getter) {
        return new WithResources<>(rGetter, () -> getter.apply(rGetter.get()));
    }

    /**
     * Variant of Result#getValue based on the standard try-with-resources
     * functionality.
     */
    private <T, E2 extends Throwable> Value<T, E2> getValue(ThrowingFunction<R, T, E2> attempt) {
        try (R r = rGetter.get()) {
            return Value.ok(attempt.apply(r));
        } catch (Throwable e) {
            return Value.err(Result.errorFound(e));
        }
    }
}