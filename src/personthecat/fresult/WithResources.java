package personthecat.fresult;

import personthecat.fresult.interfaces.*;

/**
 * A helper classed used for generating Results with two closeable resources.
 * Equivalent to using try-with-resources.
 *
 * @param <R1> The resource being consumed and closed by this handler.
 * @param <R2> The second resource being consumed and closed by this handler.
 * @param <E> The type of error to be caught by the handler.
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
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
     * This ultimately changes the type of error handled by the wrapper, but the
     * original error must be a subclass of the new error in order for it to work.
     *
     * @param attempt A function which consumes both resources and either returns
     *                a value or throws an exception.
     * @return The result of the operation.
     */
    public <T, E2 extends E> Result.Pending<T, E> of(ThrowingBiFunction<R1, R2, T, E2> attempt) {
        return Result.of(() -> {
            try (R1 r1 = this.r1Getter.get(); R2 r2 = this.r2Getter.get()) {
                return attempt.apply(r1, r2);
            } catch (Throwable e) {
                throw Result.<E>errorFound(e);
            }
        });
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which does not return a value.
     *
     * @param attempt A function which consumes both values and may throw an
     *                exception.
     * @return The result of the operation.
     */
    public <E2 extends E> Result.Pending<Void, E> of(ThrowingBiConsumer<R1, R2, E2> attempt) {
        return this.of((r1, r2) -> {
            attempt.accept(r1, r2);
            return Void.INSTANCE;
        });
    }

    /**
     * Variant of {@link #of(ThrowingBiFunction)} which only accepts the second
     * resource in the queue.
     * @param attempt A function which consumes the second resource and either
     *                returns a value or throws an exception.
     * @return The result of the operation.
     */
    public <T, E2 extends E> Result.Pending<T, E> of(ThrowingFunction<R2, T, E2> attempt) {
        return this.of((R1 r1, R2 r2) -> attempt.apply(r2));
    }

    /**
     * Variant of {@link #of(ThrowingBiConsumer)} which only accepts the second
     * resource in the queue.
     * @param attempt A function which consumes the second value and may throw
     *                an exception.
     * @return The result of the operation.
     */
    public <E2 extends E> Result.Pending<Void, E> of(ThrowingConsumer<R2, E2> attempt) {
        return this.of((r1, r2) -> { attempt.accept(r2); });
    }
}