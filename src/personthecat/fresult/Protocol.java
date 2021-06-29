package personthecat.fresult;

import personthecat.fresult.interfaces.ThrowingRunnable;
import personthecat.fresult.interfaces.ThrowingSupplier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static personthecat.fresult.Shorthand.missingProcedureEx;

/**
 * A set of procedures for handling specific error types.
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class Protocol {

    private final Set<Procedure<? extends Exception>> procedures = new HashSet<>();

    /**
     * Defines a new procedure for handling a specific type of error.
     *
     * @param type The type of error being handled.
     * @param func A function consuming the specified error, if present.
     * @return The current Protocol which now contains the procedure.
     */
    public <E extends Exception> Protocol define(Class<E> type, Consumer<E> func) {
        this.procedures.add(new Procedure<>(type, func));
        return this;
    }

    /**
     * Constructs a new Result from the supplied operation. Unlike other
     * Result wrappers, this operation is not computed lazily, as any
     * potential errors will be handled according to the protocol.
     *
     * @param attempt A function which either yields a value or throws an
     *                exception.
     * @return The result of the operation.
     */
    public <T> Result<T, Exception> of(ThrowingSupplier<T, Exception> attempt) {
        try {
            return new Result.Value<>(attempt.get());
        } catch (Exception e) {
            if (tryHandle(e)) {
                return new Result.Error<>(e);
            }
            throw missingProcedureEx(e);
        }
    }

    /**
     * Variant of {@link #of(ThrowingSupplier)} which does not yield a value.
     *
     * @param attempt A function which may throw an exception.
     * @return The result of the operation.
     */
    public Result<Void, Exception> of(ThrowingRunnable<Exception> attempt) {
        return this.of(() -> {
            attempt.run();
            return Void.INSTANCE;
        });
    }

    /**
     * Variant of {@link #of(ThrowingSupplier)} which returns the value
     * directly, wrapped in {@link Optional}.
     *
     * @param attempt A function which either yeilds a value or throws an
     *                exception.
     * @return The value yielded by the operation, wrapped in {@link Optional}.
     */
    public <T> Optional<T> get(ThrowingSupplier<T, Exception> attempt) {
        return this.of(attempt).get();
    }

    /**
     * Attempts to handle the input exception, returning whether a procedure
     * is implemented.
     */
    private boolean tryHandle(Exception e) {
        return procedures.stream().anyMatch(proc -> {
            if (proc.clazz.isInstance(e)) {
                proc.func.accept(cast(e));
                return true;
            }
            return false;
        });
    }

    @SuppressWarnings("unchecked")
    private <E extends Exception> E cast(Exception e) {
        return (E) e;
    }

    /** A procedure defined for handling a specific type of error. */
    private static class Procedure<T> {
        final Class<T> clazz;
        final Consumer<T> func;

        Procedure(Class<T> clazz, Consumer<T> func) {
            this.clazz = clazz;
            this.func = func;
        }
    }
}
