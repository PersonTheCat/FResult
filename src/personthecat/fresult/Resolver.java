package personthecat.fresult;

import personthecat.fresult.exception.MissingProcedureException;
import personthecat.fresult.functions.ThrowingSupplier;

import javax.annotation.CheckReturnValue;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static personthecat.fresult.Shorthand.missingProcedureEx;

/**
 * A set of procedures for handling specific error types.
 *
 * @param <T> The type of value being returned by this wrapper.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Resolver<T> {

    private final Set<Procedure<Throwable, T>> procedures = new HashSet<>();

    /**
     * Indicates that this type may not be instantiated from an external
     * package and is effectively sealed.
     */
    Resolver() {}

    /**
     * Defines a new procedure for handling a specific type of error.
     *
     * @param type The type of error being handled.
     * @param func A function consuming the specified error, if present.
     * @return The current Resolver which now contains the procedure.
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public <E extends Throwable> Resolver<T> resolve(final Class<E> type, final Function<E, T> func) {
        this.procedures.add((Procedure<Throwable, T>) new Procedure<>(type, func));
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
     * @return The result of the operation.
     */
    @CheckReturnValue
    public Result.Value<T, Throwable> of(final ThrowingSupplier<T, Throwable> attempt) {
        T value;
        try {
            value = attempt.get();
        } catch (Throwable e) {
            value = tryHandle(e);
        }
        return Result.ok(Objects.requireNonNull(value, "nonnull attempt"));
    }

    /**
     * Optional syntactic variant of {@link #of(ThrowingSupplier)} with equivalent
     * functionality.
     *
     * @see Resolver#of(ThrowingSupplier)
     * @throws MissingProcedureException If a caught exception is undefined.
     * @throws NullPointerException If the attempt returns null.
     * @param attempt A function which either yields a value or throws an exception.
     * @return The result of the operation.
     */
    @CheckReturnValue
    public Result<T, Throwable> suppress(final ThrowingSupplier<T, Throwable> attempt) {
        return this.of(attempt);
    }

    /**
     * Variant of {@link Protocol#of(ThrowingSupplier)} which is allowed to return null.
     * After defining procedures for this wrapper, the next ideal step is to call
     * {@link OptionalResult#defaultIfEmpty} to obtain an upgraded {@link PartialResult}.
     *
     * @param attempt A function which either yields a value, throws an exception, or
     *                returns null.
     * @return A result which may either be a value, error, or empty.
     */
    public OptionalResult<T, Throwable> nullable(final ThrowingSupplier<T, Throwable> attempt) {
        try {
            return Result.nullable(attempt.get());
        } catch (Throwable e) {
            return Result.nullable(tryHandle(e));
        }
    }

    /**
     * Variant of {@link #of(ThrowingSupplier)} which returns the value
     * directly, wrapped in {@link Optional}.
     *
     * @param attempt A function which either yields a value or throws an exception.
     * @return The value yielded by the operation, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    public Optional<T> optional(final ThrowingSupplier<T, Throwable> attempt) {
        return this.nullable(attempt).get();
    }

    /**
     *
     * @param attempt A function which either yield a value or throws an exception.
     * @return The output of this function, or else one of the provided defaults.
     */
    public T expose(final ThrowingSupplier<T, Throwable> attempt) {
        return this.of(attempt).expose();
    }

    /**
     * Attempts to handle the input exception, returning whichever value was
     * returned by the handler.
     */
    private T tryHandle(final Throwable e) {
        for (final Procedure<Throwable, T> proc : this.procedures) {
            if (proc.clazz.isInstance(e)) {
                return proc.func.apply(proc.clazz.cast(e));
            }
        }
        throw missingProcedureEx(e);
    }

    @SuppressWarnings("unchecked")
    private <E extends Throwable> E cast(final Throwable e) {
        return (E) e;
    }

    /** A procedure defined for handling a specific type of error. */
    private static class Procedure<E extends Throwable, R> {
        final Class<E> clazz;
        final Function<E, R> func;

        Procedure(final Class<E> clazz, final Function<E, R> func) {
            this.clazz = clazz;
            this.func = func;
        }
    }
}
