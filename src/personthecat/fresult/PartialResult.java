package personthecat.fresult;

import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.interfaces.ThrowingFunction;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class PartialResult<T, E extends Throwable> extends OptionalResult<T, E> {

    /**
     * This constructor indicates that {@link Result} is effectively a sealed class type.
     * It may not be extended outside of this package.
     */
    PartialResult() {}

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
    public abstract Result<T, E> ifErr(Consumer<E> f);

    /**
     * Override providing a default behavior for all child types.
     *
     * @see OptionalResult#get
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @Override
    @CheckReturnValue
    public Optional<T> get(Consumer<E> f) {
        return this.ifErr(f).get();
    }

    /**
     * Converts this wrapper into a new type when accounting for both potential outcomes.
     *
     * @param ifOk The transformer applied if a value is present in the wrapper.
     * @param ifErr The transformer applied if an error is present in the wrapper.
     * @param <U> The type of value being output by this method.
     * @return The output of either <code>ifOk</code> or <code>ifErr</code>.
     */
    @CheckReturnValue
    public abstract <U> U fold(Function<T, U> ifOk, Function<E, U> ifErr);

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
    public abstract T orElseGet(Function<E, T> f);

    /**
     * Maps to a new Result if an error is present. Use this whenever
     * your first and second attempt at retrieving a value may fail.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    public abstract PartialResult<T, E> orElseTry(ThrowingFunction<E, T, E> f);

    /**
     * @see OptionalResult#defaultIfEmpty
     * @deprecated PartialResult is never empty.
     */
    @Override
    @Deprecated
    public PartialResult<T, E> defaultIfEmpty(Supplier<T> defaultGetter) {
        return this;
    }

    /**
     * @see OptionalResult#ifEmpty
     * @deprecated PartialResult is never empty.
     */
    @Override
    @Deprecated
    public PartialResult<T, E> ifEmpty(Runnable f) {
        return this;
    }
}
