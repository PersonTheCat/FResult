package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.interfaces.*;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.f;
import static personthecat.fresult.Shorthand.runEx;
import static personthecat.fresult.Shorthand.unwrapEx;
import static personthecat.fresult.Shorthand.warn;
import static personthecat.fresult.Shorthand.wrongErrorEx;

/**
 * <h3>
 *   A counterpart to {@link Optional} used for neatly handling errors. Accepts
 * an expression which is not processed until a procedure for handling any
 * potential errors is implemented.
 * </h3><p>
 *   This class provides a functional interface capable of completely replacing
 * the standard try-catch and try-with-resources notations in Java. It has two
 * essential modes of use:
 * </p><ol>
 *     <li>As a better return type for functions that use standard error handling
 *         procedures, and</li>
 *     <li>As a wrapper around functions that do not.</li>
 * </ol><p>
 *   Basic implementations for each of the above use-cases are as follows:
 * </p><pre>
 *   // Return the product of each block.
 *   static Result<String, IOException> betterReturn() {
 *     final File f = getFile();
 *     try {
 *       return Result.ok(getContents(f));
 *     } catch (IOException e) {
 *       return Result.err(e);
 *     }
 *   }
 *
 *   // Standard conventions to be wrapped.
 *   static String toWrap() throws IOException {
 *     final File f = getFile();
 *     return getContents(f);
 *   }
 *
 *   // Create and return a new error directly.
 *   static Result<String, IOException> betterReturnAlt() {
 *     final File f = getFile();
 *     return testConditions()
 *       ? Result.ok(getContents(f))
 *       : Result.err(new IOException());
 *   }
 * </pre><p>
 *   The code which makes use of these functions would be identical in either
 * case, as follows:
 * </p><pre>{@code
 *   Result<String, IOException> r1 = betterReturn();
 *   Result<String, IOException> r2 = Result.of(Name::toWrap);
 * }</pre><p>
 *   It is worth noting that the above code always produces a new wrapper, but
 * is not guaranteed to execute. It is thereby essential that functions returning
 * new wrappers be followed by some type of handler, e.g. {@link #ifErr} or
 * {@link #expect}.
 * </p><p>
 *   Wrapping error-prone procedures in a functional interface has the modest
 * benefit of being more expressive and easier to maintain. It gives callers a
 * more convenient interface for supplying alternate values and reporting errors
 * to the end-user. However, this comes at a cost. It is certainly less safe and
 * less specific than vanilla error handling in Java, as it sometimes requires a
 * bit of runtime reflection and only accepts one error parameter by default. It
 * also requires a bit of memory overhead which likely has at least a minor
 * impact on performance. Some may consider functional error handling to be more
 * readable than imperative error handling, but this may not apply for most Java
 * developers, as it strays from the standard conventions they're used to and is
 * thus a bit of an outlier in that world.
 * </p>
 *
 * @param <T> The type of value being wrapped.
 * @param <E> The type of error being wrapped.
 * @author PersonTheCat
 */
@SuppressWarnings("unused")
public interface Result<T, E extends Throwable> extends BasicResult<T, E> {

    /**
     * Accepts an error and ignores it, while still coercing it to its lowest type, thus
     * not allowing any unspecified error types to be ignored.
     *
     * <p>e.g.</p>
     * {@code Result.of(...).ifErr(Result::IGNORE);}
     */
    static <E extends Throwable> void IGNORE(final E e) {}

    /**
     * Accepts an error and logs it as a warning. This implementation is also type safe.
     * <p>e.g.</p>
     * {@code Result.of(...).ifErr(Result::WARN);}
     */
    static <E extends Throwable> void WARN(final E e) { warn("{}", e); }

    /**
     * Accepts an error and throws it. This implementation is also type safe.
     * <p>e.g.</p>
     * {@code Result.of(...).ifErr(Result::THROW);}
     */
    static <E extends Throwable> void THROW(final E e) { throw runEx(e); }

    /**
     * A runnable method for cases in which an {@link PartialOptionalResult} is empty.
     * <p>e.g.</p>
     * {@code Result.nullable(...).ifEmpty(Result::WARN_NULL);}
     */
    static void WARN_NULL() { warn("Null value in wrapper"); }

    /**
     * Returns a generic result containing no value or error. Use this method whenever
     * no operation needs to occur, but a `Result` value is being returned.
     *
     * @param <E> The type of error being consumed by the wrapper.
     * @return A singleton result which is always OK.
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    static <E extends Throwable> Value<Void, E> ok() {
        return (Value<Void, E>) Value.OK;
    }

    /**
     * Returns a new result with a value and no errors. Use this method whenever no operation
     * needs to occur, but a value exists and must be wrapped in a <code>Result</code>.
     *
     * @param val The value being wrapped.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which is always OK.
     */
    @CheckReturnValue
    static <T, E extends Throwable> Value<T, E> ok(final T val) {
        return new Value<>(val);
    }

    /**
     * Returns a new result with an error and no value present. Use this method to enable
     * callers to respond functionally even though error handling has already occurred.
     *
     * @param e The error being wrapped.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which is always an error.
     */
    @CheckReturnValue
    static <T, E extends Throwable> Error<T, E> err(final E e) {
        return new Error<>(e);
    }

    /**
     * Returns a new result with no error or value present. Use this method to enable
     * callers to respond functionally in an environment where error handling has
     * already occurred <em>and</em> you wish to optionally return null.
     *
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which is always null.
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    static <T, E extends Throwable> OptionalResult<T, E> empty() {
        return (Empty<T, E>) Empty.INSTANCE;
    }

    /**
     * This is the primary method for constructing a new <code>Result</code> object using any
     * operation which will either yield a value or produce an error.
     *
     * <p>e.g.</p>
     * <pre>
     *   final File f = getFile();
     *   // Get the result of calling f#mkdirs,
     *   // handling a potential SecurityException.
     *   final boolean success = Result.of(f::mkdirs)
     *     .orElseGet(e -> false); // ignore SecurityExceptions, return default.
     * </pre>
     *
     * @param attempt An expression which either yields a value or throws an exception.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or an error.
     */
    @CheckReturnValue
    static <T, E extends Throwable> Pending<T, E> of(final ThrowingSupplier<T, E> attempt) {
        return new Pending<>(attempt);
    }

    /**
     * Constructs a new result from an operation which will not yield a value.
     *
     * <p>e.g.</p>
     * <pre>
     *   // Functional equivalent to a standard try-catch block.
     *   Result.of(ClassName::thisWillFail)
     *     .ifErr(e -> {...}); // Process executes here.
     * </pre>
     *
     * @see Result#of(ThrowingSupplier)
     * @param attempt An expression which does not yield a value, but may err.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be OK or an error.
     */
    @CheckReturnValue
    static <E extends Throwable> Pending<Void, E> of(final ThrowingRunnable<E> attempt) {
        return new Pending<>(wrapVoid(attempt));
    }

    /**
     * Constructs a new result from an operation which may throw <b>any exception</b>.
     *
     * <p>
     *   Unlike {@link Result#of(ThrowingSupplier)}, this method is <b>not</b> lazily-evaluated.
     *   Instead, a result is returned immediately. This is allowed because the type of error
     *   being caught by the wrapper does not have to be acknowledged when queued for any
     *   possible outcome.
     * </p>
     *
     * @param attempt An expression which either yields a value or throws an exception.
     * @param <T> The type of value being consumed by the wrapper.
     * @return A result which may either a value or any kind of exception.
     */
    @CheckReturnValue
    static <T> Result<T, Throwable> any(final ThrowingSupplier<T, Throwable> attempt) {
        try {
            return ok(attempt.get());
        } catch (Throwable t) {
            return err(t);
        }
    }

    /**
     * Constructs a result from an operation which may throw <b>any exception</b> and not return
     * a value.
     *
     * @see Result#any(ThrowingSupplier)
     * @param attempt An expression which does not yield a value, but may err.
     * @return A result which may either be OK or any kind of exception.
     */
    @CheckReturnValue
    static Result<Void, Throwable> any(final ThrowingRunnable<Throwable> attempt) {
        try {
            attempt.run();
            return ok();
        } catch (Throwable t) {
            return err(t);
        }
    }

    /**
     * Constructs a new result from a known value which may or may not be null.
     *
     * <p>e.g.</p>
     * <pre>
     *   // Use this to wrap a value which may or may not be null
     *   // Call Optional#orElse or Optional#orElseGet to substitute
     *   // the value, if null.
     *   final Object result = Result.nullable(potentiallyNullValue)
     *     .defaultIfNull(Object::new); // Alternate value if null
     *     .orElseGet(e -> Optional.of(nonNullValue)) // Alternate value if err
     * </pre>
     *
     * @param value Any given value which may be null
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    @CheckReturnValue
    static <T, E extends Throwable> OptionalResult<T, E> nullable(final T value) {
        return value == null ? Result.empty() : Result.ok(value);
    }

    /**
     * Constructs a new result from an operation which may neither err or return a value.
     *
     * <p>e.g.</p>
     * <pre>
     *   // Use this to call a function which may either throw
     *   // an exception or simply return null. Call Optional#orElse
     *   // or Optional#orElseGet to substitute the value, if null.
     *   final Object result = Result.nullable(ClassName::thisMayReturnNullOrFail)
     *     .defaultIfNull(Object::new);
     *     .expect("Error message!")
     * </pre>
     *
     * @see Result#nullable(T)
     * @param attempt An expression which either yields a value or throws an error.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    @CheckReturnValue
    static <T, E extends Throwable> PendingNullable<T, E> nullable(final ThrowingSupplier<T, E> attempt) {
        return new PendingNullable<>(attempt);
    }

    /**
     * Variant of {@link Result#nullable} which wraps the given value in Optional
     * instead of returning an {@link PartialOptionalResult}. This may be useful in some
     * cases where it is syntactically shorter to handle null values via {@link Optional}.
     *
     * @param value The value being consumed by the wrapper.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be an optional value or an error.
     */
    static <T, E extends Throwable> Result<Optional<T>, E> wrappingOptional(final T value) {
        return new Value<>(Optional.ofNullable(value));
    }

    /**
     * Variant of {@link Result#nullable} which wraps the output in Optional instead of
     * returning an instance of {@link PartialOptionalResult}. This may be useful in some cases
     * where it is syntactically shorter to handle null values via {@link Optional}.
     *
     * <p>e.g.</p>
     * <pre>
     *   final Object result = Result.wrappingOptional(() -> dangerousMethod())
     *     .expect("Error message!") // Get optional value
     *     .orElseGet(Object::new); // Supply a default value.
     * </pre>
     *
     * @param attempt An expression which either yields a value, throws an exception,
     *                or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be an optional value or an error.
     */
    static <T, E extends Throwable> Pending<Optional<T>, E> wrappingOptional(final ThrowingSupplier<T, E> attempt) {
        return new Pending<>(() -> Optional.ofNullable(attempt.get()));
    }

    /**
     * Constructs a handler for generating a new Result from an operation with resources.
     * The implementation returned, {@link WithResource}, allows one additional `with`
     * clause, enabling multiple resources to be chained, consumed, and closed. This is
     * the functional equivalent of using a standard try-with-resources block.
     *
     * <p>e.g.</p>
     * <pre>
     *   final File f = getFile();
     *   // Attempt to generate a new FileWriter and
     *   // use it write lines into `f`.
     *   Result.with(() -> new FileWriter(f))
     *     .of(writer -> writer.write("Hello World!"))
     *     .expect("Tried to write. It failed.");
     * </pre>
     *
     * @param <R> The type of resource being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @param resource An expression which yields an {@link AutoCloseable} resource.
     * @return A handle for interacting with the given resource.
     */
    @CheckReturnValue
    static <R extends AutoCloseable, E extends Throwable>
    WithResource<R, E> with(final ThrowingSupplier<R, E> resource) {
        return new WithResource<>(resource);
    }

    /**
     * Shorthand method for following a {@link #with(ThrowingSupplier)} call with a
     * {@link WithResource#of(ThrowingFunction)} call. Use this for example with
     * convoluted {@link #with} calls that require additional lines to avoid unnecessary
     * indentation.
     *
     * <p>e.g.</p>
     * <pre>
     *   final File f = getFile();
     *   Result.with(() -> new FileWriter(f), writer -> {
     *     writer.write("Hello World!");
     *   }).expect("Tried to write. It failed.");
     * </pre>
     *
     * @param resource An expression which yields an {@link AutoCloseable} resource.
     * @param attempt  An expression which consumes the resource and either yields a
     *                 value or throws an exception.
     * @param <R> The type of resource being consumed by the wrapper.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or an error.
     */
    @CheckReturnValue
    static <R extends AutoCloseable, T, E extends Throwable>
    Pending<T, E> with(final ThrowingSupplier<R, E> resource, final ThrowingFunction<R, T, E> attempt) {
        return with(resource).of(attempt);
    }

    /**
     * Shorthand method for following a {@link #with(ThrowingSupplier)} call with a
     * {@link WithResource#of} call. Use this for an alternative to
     * {@link Result#with(ThrowingSupplier, ThrowingFunction)} that does not return
     * a value.
     *
     * @see Result#with(ThrowingSupplier, ThrowingFunction)
     * @param resource An expression which yields an {@link AutoCloseable} resource.
     * @param attempt  An expression which consumes the resource and does not yield
     *                 a value, but may err.
     * @param <R> The type of resource being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be a value or an error.
     */
    @CheckReturnValue
    static <R extends AutoCloseable, E extends Throwable>
    Pending<Void, E> with(final ThrowingSupplier<R, E> resource, final ThrowingConsumer<R, E> attempt) {
        return with(resource).of(attempt);
    }

    /**
     * Creates a new protocol for handling the specified error type. Can be chained
     * with additional definitions to handle multiple different error types as needed.
     *
     * @param clazz The class of error to be handled by this definition.
     * @param func  The function used for handling this kind of error.
     * @return A new {@link Protocol} for handling the specified error type.
     */
    @CheckReturnValue
    static <E extends Exception> Protocol define(final Class<E> clazz, final Consumer<E> func) {
        return new Protocol().define(clazz, func);
    }

    /**
     * Wraps any exceptions from the array into a new Result object.
     *
     * @param results An array of result wrappers to be joined.
     * @return A new wrapper which yields no value, but throws any possible exception from
     *         the input.
     */
    @SafeVarargs
    @CheckReturnValue
    static <E extends Throwable> Result<Void, E> join(final Result<Void, E>... results) {
        for (Result<Void, E> result : results) {
            if (result.isErr()) {
                return result;
            }
        }
        return Result.ok();
    }
    /**
     * Wraps any exceptions from the array into a new Result object.
     *
     * @param results An array of result wrappers to be joined.
     * @return A new wrapper which yields no value, but throws any possible exception from
     *         the input.
     */
    @SafeVarargs
    @CheckReturnValue
    static <E extends Throwable> Pending<Void, E> join(final Pending<Void, E>... results) {
        return new Pending<>(() -> {
            for (Pending<Void, E> result : results) {
                try {
                    result.orElseThrow();
                } catch (final Throwable e) {
                    throw Result.<E>errorFound(e);
                }
            }
            return Void.INSTANCE;
        });
    }

    /**
     * Collects a series of results into a <b>mutable</b> list of their contents.
     *
     * @param results An array of result wrappers to be collected from.
     * @return A new wrapper which yields a mutable list of results, while throwing
     *         any possible exception from the input.
     */
    @SafeVarargs
    @CheckReturnValue
    static <T, E extends Throwable> Pending<List<T>, E> collect(final Result<T, E>... results) {
        return new Pending<>(() -> {
            final List<T> list = new ArrayList<>();
            for (Result<T, E> result : results) {
                list.add(result.orElseThrow());
            }
            return list;
        });
    }

    /**
     * Converts a {@link ThrowingRunnable} type with no return value into a {@link ThrowingSupplier}
     * which implicitly returns an instance of {@link Void}.
     *
     * @param attempt A function which may throw a checked exception.
     * @param <E> The type of exception being thrown by the function.
     * @return A supplier wrapping this runnable function.
     */
    static <E extends Throwable> ThrowingSupplier<Void, E> wrapVoid(final ThrowingRunnable<E> attempt) {
        return () -> {
            attempt.run();
            return Void.INSTANCE;
        };
    }

    /**
     * Converts a {@link ThrowingConsumer} type with no return value into a {@link ThrowingFunction}
     * which implicitly returns an instance of {@link Void}.
     *
     * @param attempt A function which may either yield a value or throw a checked exception.
     * @param <R> The type of resource being consumed by the function.
     * @param <E> The type of exception being thrown by the function.
     * @return A function wrapping this consumer function.
     */
    static <R, E extends Throwable> ThrowingFunction<R, Void, E> wrapVoid(final ThrowingConsumer<R, E> attempt) {
        return r -> {
            attempt.accept(r);
            return Void.INSTANCE;
        };
    }

    /**
     * Converts a {@link ThrowingBiConsumer} type with no return value into a {@link ThrowingBiFunction}
     * which implicitly returns an instance of {@link Void}.
     *
     * @param attempt A function which may either yield a value or throw a checked exception.
     * @param <R1> The first type of resource being consumed by the function.
     * @param <R2> The second type of resource being consumed by the function.
     * @param <E> The type of exception being thrown by the function.
     * @return A function wrapping this consumer.
     */
    static <R1, R2, E extends Throwable>
    ThrowingBiFunction<R1, R2, Void, E> wrapVoid(final ThrowingBiConsumer<R1, R2, E> attempt) {
        return (r1, r2) -> {
            attempt.accept(r1, r2);
            return Void.INSTANCE;
        };
    }

    /**
     * Returns whether the expected type of error occurred in the process.
     *
     * e.g.
     * <pre>
     *   final Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it errs.
     *   if (result.isErr()) {
     *       ...
     *   }
     * </pre>
     *
     * @return true, if an error is present.
     */
    @CheckReturnValue
    boolean isErr();

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
    Result<T, E> ifErr(final Consumer<E> f);

    /**
     * Returns whether a value was yielded or no error occurred.
     *
     * <p>e.g.</p>
     * <pre>
     *   final Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it does not err.
     *   if (result.isOk()) {
     *       ...
     *   }
     * <pre>
     *
     * @return true, if a value is present.
     */
    @CheckReturnValue
    boolean isOk();

    /**
     * Accepts an expression for what to do in the event of no error
     * being present. Use this whenever you want to functionally handle
     * both code paths (i.e. error vs. value).
     *
     * @param f A function consuming the value, if present.
     * @return This, or else a complete {@link Result}.
     */
    @CheckReturnValue
    Result<T, E> ifOk(final Consumer<T> f);

    /**
     * Attempts to retrieve the underlying value, if present, while also accounting for
     * any potential errors.
     *
     * e.g.
     * <pre>
     *   Result.of(() -> getValueOrFail())
     *     .get(e -> {...})
     *     .ifPresent(t -> {...});
     * </pre>
     *
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<T> get(final Consumer<E> func);

    /**
     * Effectively casts this object into a standard Optional instance.
     * Prefer calling {@link Result#get(Consumer)}, as this removes the need for
     * any implicit error checking.
     *
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<T> get();

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    Optional<E> getErr();

    /**
     * Converts this wrapper into a new type when accounting for both potential outcomes.
     *
     * @param ifOk The transformer applied if a value is present in the wrapper.
     * @param ifErr The transformer applied if an error is present in the wrapper.
     * @param <U> The type of value being output by this method.
     * @return The output of either <code>ifOk</code> or <code>ifErr</code>.
     */
    @CheckReturnValue
    <U> U fold(final Function<T, U> ifOk, final Function<E, U> ifErr);

    /**
     * Yields the underlying value or else the input. This is equivalent to
     * running `getResult().get(e -> {...}).orElse();`
     *
     * @return The underlying value, or else the input.
     */
    @CheckReturnValue
    T orElse(final T val);

    /**
     * Variant of {@link PartialResult#orElseGet(Function)} which does not specifically
     * consume the error.
     *
     * @param f A function which supplies a new value if none is present in the
     *             wrapper.
     * @return The underlying value, or else func.get().
     */
    @CheckReturnValue
    T orElseGet(final Supplier<T> f);

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
    T orElseGet(final Function<E, T> f);

    /**
     * Maps to a new Result if an error is present. Use this whenever
     * your first and second attempt at retrieving a value may fail.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    PartialResult<T, E> orElseTry(final ThrowingFunction<E, T, E> f);

    /**
     * Transposes a result with an optional value into an optional result.
     *
     * @throws ClassCastException if the underlying value is not wrapped in {@link Optional<U>}.
     * @return A new Result wrapping the value directly, itself wrapped in {@link Optional}.
     */
    @CheckReturnValue
    <U> Optional<Result<U, E>> transpose();

    /**
     * Replaces the underlying value with the result of func.apply().
     * Use this whenever you need to map a potential value to a
     * different value.
     *
     * <p>e.g.</p>
     * <pre>
     *   final int secretArrayLength = Result.of(() -> getSecretArray())
     *      .get(e -> {...}) // Handle any errors.
     *      .map(array -> array.length) // Return the length instead.
     *      .orElse(0); // Didn't work. Call Optional#orElse().
     * </pre>
     *
     * @param f The mapper applied to the value.
     * @return A new Result with its value mapped.
     */
    @CheckReturnValue
    <M> Result<M, E> map(final Function<T, M> f);

    /**
     * Replaces the underlying error with the result of func.apply().
     * Use this whenever you need to map a potential error to another
     * error.
     *
     * @param f The mapper applied to the error.
     * @return A new Result with its error mapped.
     */
    @CheckReturnValue
    <E2 extends Throwable> Result<T, E2> mapErr(final Function<E, E2> f);

    /**
     * Replaces the entire value with a new result, if present.
     * Use this whenever you need to map a potential value to
     * another function which yields a Result.
     *
     * @param f A function which yields a new Result wrapper
     *             if a value is present.
     * @return The new function yielded, if a value is present,
     *         else this.
     */
    @CheckReturnValue
    Result<T, E> flatMap(final Function<T, Result<T, E>> f);

    /**
     * Replaces the entire value with a new result, if present.
     * Use this whenever you need to map a potential error to
     * another function which yields a Result.
     *
     * @param f A function which yields a new Result wrapper
     *             if an error is present.
     * @return The new function yielded, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    Result<T, E> flatMapErr(final Function<E, Result<T, E>> f);

    /**
     * Attempts to retrieve the underlying value, asserting that one must exist.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    default T unwrap() {
        return this.expect("Attempted to unwrap a result with no value.");
    }

    /**
     * Attempts to retrieve the underlying error, asserting that one must exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @CheckReturnValue
    default E unwrapErr() {
        return this.expectErr("Attempted to unwrap a result with no error.");
    }

    /**
     * Yields the underlying value, throwing a convenient, generic exception, if an
     * error occurs.
     *
     * e.g.
     * <pre>
     *   // Runs an unsafe process, wrapping any original errors.
     *   Object result = getResult()
     *     .expect("Unable to get value from result.");
     * </pre>
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @param message The message to display in the event of an error.
     * @return The underlying value.
     */
    T expect(final String message);

    /**
     * Formatted variant of {@link #expect}.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying value
     */
    default T expectF(final String message, final Object... args) {
        return expect(f(message, args));
    }

    /**
     * Yields the underlying error, throwing a generic exception if no error is present.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @return The underlying error.
     */
    @CheckReturnValue
    E expectErr(String message);

    /**
     * Formatted variant of {@link #expectErr}.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying error.
     */
    @CheckReturnValue
    default E expectErrF(final String message, final Object... args) {
        return this.expectErr(f(message, args));
    }

    /**
     * Variant of {@link #unwrap} which throws the original error, if applicable.
     *
     * @throws E The original error, if present.
     * @return The underlying value.
     */
    default T orElseThrow() throws E {
        this.throwIfErr();
        return unwrap();
    }

    /**
     * If an error is present, it will be thrown by this method.
     *
     * @throws E The original error, if present.
     */
    void throwIfErr() throws E;

    /**
     * An implementation of {@link Result} which is the outcome of a successful operation.
     *
     * @param <T> The type of value being wrapped
     * @param <E> A dummy parameter for the call site
     */
    class Value<T, E extends Throwable> implements PartialResult<T, E>, Result<T, E>, OptionalResult<T, E> {

        private static final Value<Void, ?> OK = new Value<>(Void.INSTANCE);

        private final T value;

        private Value(final T value) {
            this.value = Objects.requireNonNull(value, "Value may not be null");
        }

        /**
         * Returns the underlying value with no wrapper. This is a safe operation
         * when a given {@link Result} is known to be a {@link Value}.
         *
         * @return The underlying value.
         */
        public T expose() {
            return this.value;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Value<T, E> defaultIfEmpty(final Supplier<T> defaultGetter) {
            return this;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isEmpty() {
            return false;
        }

        /**
         * @deprecated Never runs..
         */
        @Override
        @Deprecated
        public Value<T, E> ifEmpty(final Runnable f) {
            return this;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isErr() {
            return false;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isErr(final Class<? super E> clazz) {
            return false;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isAnyErr() {
            return false;
        }

        /**
         * @deprecated Never runs.
         */
        @Override
        @Deprecated
        public Value<T, E> ifErr(Consumer<E> f) {
            return this;
        }

        /**
         * @deprecated Always returns true.
         */
        @Override
        @Deprecated
        public boolean isOk() {
            return true;
        }

        /**
         * @deprecated Always runs.
         */
        @Override
        @Deprecated
        public Value<T, E> ifOk(final Consumer<T> f) {
            f.accept(this.value);
            return this;
        }

        /**
         * @deprecated Always returns full. Use {@link Value#expose}.
         */
        @Override
        @Deprecated
        public Optional<T> get(final Consumer<E> func) {
            return Optional.of(this.value);
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public Optional<T> get() {
            return Optional.of(this.value);
        }

        /**
         * @deprecated Always returns empty.
         */
        @Override
        @Deprecated
        public Optional<E> getErr() {
            return Optional.empty();
        }

        /**
         * @deprecated Always returns empty.
         */
        @Override
        @Deprecated
        public Optional<Throwable> getAnyErr() {
            return Optional.empty();
        }

        /**
         * @deprecated Always returns <code>ifOk#apply</code>
         */
        @Override
        @Deprecated
        public <U> U fold(final Function<T, U> ifOk, final Function<E, U> ifErr) {
            return ifOk.apply(this.value);
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T expect(final String message) {
            return this.value;
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        public T expectF(final String message, final Object... args) {
            return this.value;
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public E expectErr(final String message) {
            throw unwrapEx(message);
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public E expectErrF(final String message, final Object... args) {
            throw unwrapEx(f(message, args));
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public void expectEmpty(final String message) {
            throw new AssertionError(message);
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T orElseThrow() {
            return this.value;
        }

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        public void throwIfErr() {}

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T orElse(final T val) {
            return this.value;
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T orElseGet(final Supplier<T> f) {
            return this.value;
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T orElseGet(final Function<E, T> f) {
            return this.value;
        }

        /**
         * @deprecated Nothing to try.
         */
        @Override
        @Deprecated
        public PartialResult<T, E> orElseTry(final ThrowingFunction<E, T, E> f) {
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Optional<Result<U, E>> transpose() {
            try {
                return ((Optional<U>) this.value).map(Result::ok);
            } catch (ClassCastException e) {
                throw new ClassCastException("Underlying value not wrapped in Optional<U>");
            }
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public <M> Result<M, E> map(final Function<T, M> f) {
            return new Value<>(f.apply(this.value));
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public <E2 extends Throwable> Result<T, E2> mapErr(final Function<E, E2> f) {
            return new Value<>(this.value);
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public Result<T, E> flatMap(final Function<T, Result<T, E>> f) {
            return f.apply(this.value);
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Result<T, E> flatMapErr(final Function<E, Result<T, E>> f) {
            return this;
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T unwrap() {
            return this.value;
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public E unwrapErr() {
            throw unwrapEx("Attempted to unwrap a result with no error.");
        }
    }

    /**
     * An implementation of {@link Result} which is the outcome of a failure.
     *
     * @param <T> A dummy parameter for the call site
     * @param <E> The type of error being wrapped
     */
    class Error<T, E extends Throwable> implements Result<T, E>, OptionalResult<T, E> {

        private final E error;

        private Error(final E error) {
            this.error = Objects.requireNonNull(error, "Error may not be null");
        }

        /**
         * Returns the underlying error with no wrapper. This is a safe operation
         * when a given {@link Result} is known to be an {@link Error}.
         *
         * @return The underlying error.
         */
        public E expose() {
            return this.error;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Error<T, E> defaultIfEmpty(final Supplier<T> defaultGetter) {
            return this;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isEmpty() {
            return false;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Error<T, E> ifEmpty(final Runnable f) {
            return this;
        }

        /**
         * @deprecated Always returns true.
         */
        @Override
        @Deprecated
        public boolean isErr() {
            return true;
        }

        /**
         * @deprecated Always an error.
         */
        @Override
        @Deprecated
        public Error<T, E> ifErr(final Consumer<E> f) {
            try {
                f.accept(this.error);
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
            return this;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isOk() {
            return false;
        }

        /**
         * @deprecated Never runs.
         */
        @Override
        @Deprecated
        public Error<T, E> ifOk(final Consumer<T> f) {
            return this;
        }

        /**
         * @deprecated Always returns empty.
         */
        @Override
        @Deprecated
        public Optional<T> get(final Consumer<E> func) {
            return Optional.empty();
        }

        /**
         * @deprecated Always empty.
         */
        @Override
        @Deprecated
        public Optional<T> get() {
            return Optional.empty();
        }

        /**
         * @deprecated Always present - Use {@link Error#expose}.
         */
        @Override
        public Optional<E> getErr() {
            return Optional.of(this.error);
        }

        /**
         * @deprecated Always returns <code>ifErr#apply</code>.
         */
        @Override
        @Deprecated
        public <U> U fold(final Function<T, U> ifOk, final Function<E, U> ifErr) {
            try {
                return ifErr.apply(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public T expect(final String message) {
            throw unwrapEx(message);
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public T expectF(final String message, final Object... args) {
            throw unwrapEx(f(message, args));
        }

        /**
         * @deprecated Call {@link Error#expose} instead.
         */
        @Override
        @Deprecated
        public E expectErr(final String message) {
            return this.error;
        }

        /**
         * @deprecated Call {@link Error#expose} instead.
         */
        @Override
        @Deprecated
        public E expectErrF(final String message, final Object... args) {
            return this.error;
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public void expectEmpty(final String message) {
            throw new AssertionError(message);
        }

        /**
         * @deprecated Call {@link Error#expose} instead.
         */
        @Override
        @Deprecated
        public T orElseThrow() throws E {
            throw this.error;
        }

        /**
         * @deprecated Call {@link Error#expose} instead.
         */
        @Override
        @Deprecated
        public void throwIfErr() throws E {
            throw this.error;
        }

        /**
         * @deprecated Always returns val.
         */
        @Override
        @Deprecated
        public T orElse(final T val) {
            return val;
        }

        /**
         * @deprecated Always returns val.
         */
        @Override
        @Deprecated
        public T orElseGet(final Supplier<T> f) {
            return f.get();
        }

        /**
         * @deprecated Always returns output of <code>f</code>.
         */
        @Override
        @Deprecated
        public T orElseGet(final Function<E, T> f) {
            try {
                return f.apply(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        /**
         * @deprecated Always runs function.
         */
        @Override
        @Deprecated
        public Pending<T, E> orElseTry(final ThrowingFunction<E, T, E> f) {
            try {
                return Result.of(() -> f.apply(this.error));
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        @Override
        public <U> Optional<Result<U, E>> transpose() {
            try {
                return Optional.of(new Error<>(this.error));
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        /**
         * @deprecated Needless allocation.
         */
        @Override
        @Deprecated
        public <M> Result<M, E> map(final Function<T, M> f) {
            try {
                return new Error<>(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public <E2 extends Throwable> Result<T, E2> mapErr(final Function<E, E2> f) {
            try {
                return new Error<>(f.apply(this.error));
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Result<T, E> flatMap(final Function<T, Result<T, E>> f) {
            return this;
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public Result<T, E> flatMapErr(final Function<E, Result<T, E>> f) {
            try {
                return f.apply(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public T unwrap() {
            throw unwrapEx("Attempted to unwrap a result with no value.");
        }

        /**
         * @deprecated Call {@link Error#expose} instead.
         */
        @Override
        @Deprecated
        public E unwrapErr() {
            return this.error;
        }
    }

    /**
     * An implementation of {@link OptionalResult} which is the outcome of neither success
     * nor an error. Always returned as a singleton instance.
     *
     * @param <T> A dummy parameter for the call site
     * @param <E> The type of error being wrapped
     */
    class Empty<T, E extends Throwable> implements OptionalResult<T, E>, PartialOptionalResult<T, E> {

        static final Empty<?, ?> INSTANCE = new Empty<>();

        private Empty() {}

        @Override
        public Value<T, E> defaultIfEmpty(final Supplier<T> defaultGetter) {
            return new Value<>(Objects.requireNonNull(defaultGetter.get(), "Default getter"));
        }

        /**
         * @deprecated Always returns true.
         */
        @Override
        @Deprecated
        public boolean isEmpty() {
            return true;
        }

        /**
         * @deprecated Always runs.
         */
        @Override
        @Deprecated
        @CheckReturnValue
        public Empty<T, E> ifEmpty(final Runnable f) {
            f.run();
            return this;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isErr() {
            return false;
        }

        /**
         * @deprecated Never runs.
         */
        @Override
        @Deprecated
        public Empty<T, E> ifErr(final Consumer<E> f) {
            return this;
        }
        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isOk() {
            return false;
        }

        /**
         * @deprecated Never runs.
         */
        @Override
        @Deprecated
        public Empty<T, E> ifOk(final Consumer<T> f) {
            return this;
        }

        /**
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isErr(final Class<? super E> clazz) {
            return false;
        }

        /**
         * @deprecated Always returns true.
         */
        @Override
        @Deprecated
        @CheckReturnValue
        public boolean isAnyErr() {
            return true;
        }

        /**
         * @deprecated Always returns empty.
         */
        @Override
        @Deprecated
        @CheckReturnValue
        public Optional<T> get(final Consumer<E> func) {
            return Optional.empty();
        }

        /**
         * @deprecated Always returns empty.
         */
        @Override
        @Deprecated
        public Optional<T> get() {
            return Optional.empty();
        }

        /**
         * @deprecated Always returns empty.
         */
        @Override
        @Deprecated
        public Optional<E> getErr() {
            return Optional.empty();
        }

        /**
         * @deprecated Always returns val.
         */
        @Override
        @Deprecated
        public T orElse(final T val) {
            return val;
        }

        /**
         * @deprecated Always returns val.
         */
        @Override
        @Deprecated
        public T orElseGet(final Supplier<T> f) {
            return f.get();
        }

        /**
         * @deprecated Always returns empty.
         */
        @Override
        @Deprecated
        public Optional<Throwable> getAnyErr() {
            return Optional.empty();
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public T unwrap() {
            throw unwrapEx("Attempted to unwrap a result with no value.");
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public E unwrapErr() {
            throw unwrapEx("Attempted to unwrap a result with no error.");
        }

        /**
         * @deprecated Does nothing.
         */
        @Override
        @Deprecated
        public void assertEmpty() {}

        /**
         * @deprecated Always throws NPE.
         */
        @Override
        @Deprecated
        public T expect(final String message) {
            throw new NullPointerException(message);
        }

        /**
         * @deprecated Always throws NPE.
         */
        @Override
        @Deprecated
        public T expectF(final String message, final Object... args) {
            throw new NullPointerException(f(message, args));
        }

        /**
         * @deprecated Always throws NPE.
         */
        @Override
        @Deprecated
        public E expectErr(final String message) {
            throw new NullPointerException(message);
        }

        /**
         * @deprecated Always throws NPE.
         */
        @Override
        @Deprecated
        public E expectErrF(final String message, final Object... args) {
            throw new NullPointerException(f(message, args));
        }

        /**
         * @deprecated Does nothing.
         */
        @Override
        @Deprecated
        public void expectEmpty(final String message) {}

        /**
         * @deprecated Does nothing.
         */
        @Override
        @Deprecated
        public void expectEmptyF(final String message, final Object... args) {}

        /**
         * @deprecated Always throws NPE.
         */
        @Override
        @Deprecated
        public T orElseThrow() {
            throw new NullPointerException("No value in wrapper.");
        }

        /**
         * @deprecated Always throws NPE.
         */
        @Override
        @Deprecated
        public void throwIfErr() {
            throw new NullPointerException("No value in wrapper");
        }
    }

    /**
     * Specifications for constructing a new {@link Result}.
     * <p>
     *     When a caller receives this type, they should hopefully be aware that it <b>does
     *   not contain a value or an error</b>, as the procedure has most likely not yet taken
     *   place. Instead, the caller must apply a handler method, which will then lazily
     *   execute the procedure and return a standard {@link Result} wrapper.
     * </p>
     * @param <T> The type of value being wrapped.
     * @param <E> The type of error being wrapped.
     */
    class Pending<T, E extends Throwable> implements PartialResult<T, E> {

        private final ThrowingSupplier<T, E> resultGetter;
        private final Supplier<T> defaultGetter;
        private volatile Result<T, E> result = null;

        private Pending(final ThrowingSupplier<T, E> getter) {
            this(getter, null);
        }

        private Pending(final ThrowingSupplier<T, E> resultGetter, final Supplier<T> defaultGetter) {
            this.resultGetter = resultGetter;
            this.defaultGetter = defaultGetter;
        }

        /**
         * This method is the safest way to handle an incoming {@link Result.Pending}.
         * <p>
         *   It provides a contract of acknowledgement that there may be an error within this
         *   wrapper. More importantly, it coerces the underlying error to the expected type
         *   at the call site, which can be handled by this wrapper.
         * </p>
         * @throws WrongErrorException If the underlying error is an unexpected type.
         * @param f A function consuming the error, if present.
         * @return A concrete, evaluated {@link Result}.
         */
        @Override
        public Result<T, E> ifErr(final Consumer<E> f) {
            return this.execute().ifErr(f);
        }

        @Override
        public Optional<Throwable> getAnyErr() {
            return this.execute().getErr().map(Function.identity());
        }

        @Override
        @CheckReturnValue
        public boolean isErr(final Class<? super E> clazz) {
            return checkError(this.execute(), clazz);
        }

        @CheckReturnValue
        public boolean isAnyErr() {
            return this.execute().isErr();
        }

        @Override
        @CheckReturnValue
        public <U> U fold(final Function<T, U> ifOk, final Function<E, U> ifErr) {
            return this.execute().fold(ifOk, ifErr);
        }

        @Override
        public T expect(final String message) {
            return this.execute().expect(message);
        }

        @Override
        public Throwable expectErr(final String message) {
            return this.execute().expectErr(message);
        }

        @Override
        public Throwable expectErrF(final String message, final Object... args) {
            return this.execute().expectErrF(message, args);
        }

        @Override
        public void throwIfErr() throws E {
            this.execute().throwIfErr();
        }

        @Override
        @CheckReturnValue
        public T orElseGet(final Function<E, T> f) {
            return this.execute().orElseGet(f);
        }

        @Override
        @CheckReturnValue
        public PartialResult<T, E> orElseTry(final ThrowingFunction<E, T, E> f) {
            return this.execute().orElseTry(f);
        }

        /**
         * Lazily initializes and returns the underlying result object contained within this
         * partial wrapper. This method is and must always be executed in some way in order
         * for the throwing process being wrapped by it to run.
         *
         * For internal callers only: use this method as an accessor for {@link this#result}.
         * <b>Do not</b> access the field directly.
         *
         * @return A {@link Result} type indicating the newly-discovered outcome.
         */
        private synchronized Result<T, E> execute() {
            if (this.result != null) {
                return this.result;
            }
            final T value;
            try {
                value = this.resultGetter.get();
            } catch (final Throwable e) {
                return this.result = new Error<>(errorFound(e));
            }

            if (value != null) {
                return this.result = new Value<>(value);
            }
            final Supplier<T> getter = Objects.requireNonNull(this.defaultGetter, "nonnull attempt");
            final T defaultValue = Objects.requireNonNull(this.defaultGetter.get(), "default is null");
            return this.result = new Value<>(defaultValue);
        }
    }

    /**
     * Variant of {@link Pending} which is allowed to wrap null values.
     * <p>
     *   As with {@link Pending}, you should be aware that this type does <b>not contain
     *   a value or an error</b>. Ideally, you would immediately handle the cases where
     *   an outcome is either null or an error.
     * </p><p>
     *   For example,
     * </p><pre>
     *   // A complete result which may never be null or contain a non-reifiable error.
     *   final Result<String, RuntimeException> result = Result.nullable(() -> null)
     *     .defaultIfEmpty(() -> "Hello, World!") // Get a Result$Pending
     *     .ifErr(() -> log.warn("oops!)); // Get a complete Result.
     * </pre>
     * @see Pending
     * @param <T> The type of value being wrapped.
     * @param <E> The type of error being wrapped.
     */
    class PendingNullable<T, E extends Throwable> implements PartialOptionalResult<T, E> {

        private final ThrowingSupplier<T, E> resultGetter;
        private volatile OptionalResult<T, E> result = null;

        private PendingNullable(final ThrowingSupplier<T, E> resultGetter) {
            this.resultGetter = resultGetter;
        }

        @Override
        @CheckReturnValue
        public PartialResult<T, E> defaultIfEmpty(final Supplier<T> defaultGetter) {
            return new Pending<>(this.resultGetter, defaultGetter);
        }

        @Override
        @CheckReturnValue
        public PartialOptionalResult<T, E> ifEmpty(final Runnable f) {
            this.execute().ifEmpty(f);
            return this;
        }

        @Override
        @CheckReturnValue
        public boolean isErr(final Class<? super E> clazz) {
            return checkError(this.execute(), clazz);
        }

        @Override
        @CheckReturnValue
        public boolean isAnyErr() {
            return this.execute().isErr();
        }

        @Override
        @CheckReturnValue
        public Optional<T> get(Consumer<E> f) {
            return this.execute().get(f);
        }

        @Override
        public Optional<Throwable> getAnyErr() {
            return this.execute().getErr().map(Function.identity());
        }

        @Override
        public T expect(final String message) {
            return this.execute().expect(message);
        }

        @Override
        public void expectEmpty(final String message) {
            this.execute().expectEmpty(message);
        }

        @Override
        public Throwable expectErr(final String message) {
            return this.execute().expectErr(message);
        }

        @Override
        public Throwable expectErrF(final String message, final Object... args) {
            return this.execute().expectErrF(message, args);
        }

        @Override
        public void throwIfErr() throws Throwable {
            this.execute().throwIfErr();
        }

        /**
         * Variant of {@link Pending#execute} which does not support default values.
         *
         * @return An {@link OptionalResult} type indicating the newly-discovered outcome.
         */
        private synchronized OptionalResult<T, E> execute() {
            if (this.result != null) {
                return this.result;
            }
            try {
                return this.result = Result.nullable(this.resultGetter.get());
            } catch (final Throwable e) {
                return this.result = new Error<>(errorFound(e));
            }
        }
    }

    /**
     * Determines whether this wrapper contains the expected type of error.
     *
     * @throws WrongErrorException if the result contains an unexpected error.
     */
    @SuppressWarnings("rawtypes")
    static <T, E extends Throwable> boolean checkError(final BasicResult<T, E> result, final Class<? super E> clazz) {
        if (result instanceof Error) {
            final Throwable error = ((Error) result).expose();
            if (!clazz.isInstance(error)) {
                throw wrongErrorFound(error);
            }
            return true;
        }
        return false;
    }

    /** Attempts to cast the error into the appropriate subclass. */
    @SuppressWarnings("unchecked")
    static <E extends Throwable> E errorFound(final Throwable err) {
        try {
            return (E) err;
        } catch (final ClassCastException e) {
            throw wrongErrorFound(err);
        }
    }

    /** Forwards `err` and informs the user that the wrong kind of error was caught. */
    static WrongErrorException wrongErrorFound(final Throwable err) {
        return wrongErrorEx("Wrong type of error caught by wrapper.", err);
    }
}
