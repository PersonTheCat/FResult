package personthecat.fresult;

import personthecat.fresult.exception.MissingProcedureException;
import personthecat.fresult.functions.ThrowingRunnable;
import personthecat.fresult.functions.ThrowingSupplier;

import javax.annotation.CheckReturnValue;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static personthecat.fresult.Shorthand.missingProcedureEx;

/**
 * A set of procedures for handling specific error types.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Protocol {

    private final Set<Procedure<? extends Throwable>> procedures = new HashSet<>();

    /**
     * Defines a new procedure for handling a specific type of error.
     *
     * @param type The type of error being handled.
     * @param func A function consuming the specified error, if present.
     * @return The current Protocol which now contains the procedure.
     */
    @CheckReturnValue
    public <E extends Throwable> Protocol define(final Class<E> type, Consumer<E> func) {
        this.procedures.add(new Procedure<>(type, func));
        return this;
    }

    /**
     * Constructs a new Result from the supplied operation. Unlike other Result wrappers,
     * this operation is not computed lazily, as any potential errors will be handled
     * according to the protocol.
     *
     * @throws MissingProcedureException If a caught exception is undefined.
     * @throws NullPointerException If the attempt returns null.
     * @param attempt A function which either yields a value or throws an exception.
     * @param <T> The type of value expected from this procedure.
     * @return The result of the operation.
     */
    @CheckReturnValue
    public <T> Result<T, Throwable> of(final ThrowingSupplier<T, Throwable> attempt) {
        final T value;
        try {
            value = attempt.get();
        } catch (Throwable e) {
            if (tryHandle(e)) {
                return Result.err(e);
            }
            throw missingProcedureEx(e);
        }
        return Result.ok(Objects.requireNonNull(value, "nonnull attempt"));
    }

    /**
     * Variant of {@link #of(ThrowingSupplier)} which does not yield a value.
     *
     * @param attempt A function which may throw an exception.
     * @return The result of the operation.
     */
    @CheckReturnValue
    public Result<Void, Throwable> of(final ThrowingRunnable<Throwable> attempt) {
        return this.of(Result.wrapVoid(attempt));
    }

    /**
     * Optional syntactic variant of {@link #of(ThrowingSupplier)} with equivalent
     * functionality.
     *
     * @see Protocol#of(ThrowingSupplier)
     * @throws MissingProcedureException If a caught exception is undefined.
     * @throws NullPointerException If the attempt returns null.
     * @param attempt A function which either yields a value or throws an exception.
     * @param <T> The type of value expected from this procedure.
     * @return The result of the operation.
     */
    @CheckReturnValue
    public <T> Result<T, Throwable> suppress(final ThrowingSupplier<T, Throwable> attempt) {
        return this.of(attempt);
    }

    /**
     * Optional syntactic variant of {@link #of(ThrowingRunnable)} with equivalent
     * functionality.
     *
     * @see Protocol#of(ThrowingRunnable)
     * @param attempt A function which may throw an exception.
     * @return The result of the operation.
     */
    @CheckReturnValue
    public Result<Void, Throwable> suppress(final ThrowingRunnable<Throwable> attempt) {
        return this.of(attempt);
    }

    /**
     * Variant of {@link Protocol#of(ThrowingSupplier)} which is allowed to return null.
     * After defining procedures for this wrapper, the next ideal step is to call
     * {@link OptionalResult#defaultIfEmpty} to obtain an upgraded {@link PartialResult}.
     *
     * @param attempt A function which either yields a value, throws an exception, or
     *                returns null.
     * @param <T> The type of value expected from this procedure.
     * @return A result which may either be a value, error, or empty.
     */
    public <T> OptionalResult<T, Throwable> nullable(final ThrowingSupplier<T, Throwable> attempt) {
        try {
            return Result.nullable(attempt.get());
        } catch (Throwable e) {
            if (tryHandle(e)) {
                return Result.err(e);
            }
            throw missingProcedureEx(e);
        }
    }

    /**
     * Variant of {@link Protocol#nullable} which wraps the given value in Optional
     * instead of returning an {@link OptionalResult}. This may be useful in some
     * cases where it is syntactically shorter to handle null values via {@link Optional}.
     *
     * @param attempt A function which either yields a value, throws an exception, or
     *                returns null.
     * @param <T> The type of value expected from this procedure.
     * @return A result which may either be an optional value or an error.
     */
    public <T> Result<Optional<T>, Throwable> wrappingOptional(final ThrowingSupplier<T, Throwable> attempt) {
        return this.of(() -> Optional.ofNullable(attempt.get()));
    }

    /**
     * Variant of {@link #of(ThrowingSupplier)} which returns the value
     * directly, wrapped in {@link Optional}.
     *
     * @param attempt A function which either yields a value or throws an
     *                exception.
     * @return The value yielded by the operation, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    public <T> Optional<T> get(final ThrowingSupplier<T, Throwable> attempt) {
        return this.of(attempt).get();
    }

    /**
     * Variant of {@link #of(ThrowingRunnable)} which executes the procedure
     * immediately and returns no output.
     *
     * @throws MissingProcedureException If a caught exception is undefined.
     * @param attempt A function which either yields a value or throws an
     *                exception.
     */
    public void run(final ThrowingRunnable<Throwable> attempt) {
        try {
            attempt.run();
        } catch (Throwable e) {
            if (!tryHandle(e)) {
                throw missingProcedureEx(e);
            }
        }
    }

    /**
     * Attempts to handle the input exception, returning whether a procedure
     * is implemented.
     */
    private boolean tryHandle(final Throwable e) {
        return procedures.stream().anyMatch(proc -> {
            if (proc.clazz.isInstance(e)) {
                proc.func.accept(cast(e));
                return true;
            }
            return false;
        });
    }

    @SuppressWarnings("unchecked")
    private <E extends Throwable> E cast(final Throwable e) {
        return (E) e;
    }

    /** A procedure defined for handling a specific type of error. */
    private static class Procedure<T> {
        final Class<T> clazz;
        final Consumer<T> func;

        Procedure(final Class<T> clazz, final Consumer<T> func) {
            this.clazz = clazz;
            this.func = func;
        }
    }
}
