package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.functions.*;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.checkError;
import static personthecat.fresult.Shorthand.errorFound;
import static personthecat.fresult.Shorthand.f;
import static personthecat.fresult.Shorthand.runEx;
import static personthecat.fresult.Shorthand.unwrapEx;
import static personthecat.fresult.Shorthand.warn;
import static personthecat.fresult.Shorthand.wrongErrorEx;

/**
 * <h2>
 *   A powerful and expressive counterpart to {@link Optional} use for neatly
 *   handling errors.
 * </h2><p>
 *   This interface is capable of completely replacing the standard try-catch
 *   and try-with-resources notations in java. It has two essential modes of
 *   use:
 * </p><ol>
 *     <li>As a better return type for functions that use standard error handling
 *         procedures, and</li>
 *     <li>As a wrapper around functions that do not.</li>
 * </ol><h2>
 *     Using {@link Result} as a better return type
 * </h2><pre>
 *     // Return the product of each block.
 *     public static Result<String, IOException> betterReturn() {
 *       final File f = getFile();
 *       try {
 *         return Result.ok(getContents(f));
 *       } catch (final IOException e) {
 *         return Result.err(e);
 *       }
 *     }
 *
 *     // Create and return a new error directly.
 *     public static Result<String, IOException> betterReturnAlt() {
 *       final File f = getFile();
 *       return testConditions(f)
 *         ? Result.ok(getContents(f))
 *         : Result.err(new IOException());
 *     }
 * </pre><h3>
 *     Implementation
 * </h3><pre>
 *     // Handle all outcome scenarios.
 *     final Result<String, IOException> r1 = betterReturn()
 *       .ifErr(Result::WARN) // Output a warning message
 *       .ifOk(ContentConsumer::apply); // Consume the text output
 *
 *     // Transform the data into a common type.
 *     final int numLines = r1
 *       .fold(t -> t.lines().size(), e -> 0);
 *
 *     // Immediately supply an alternate value.
 *     final String output = betterReturnAlt()
 *       .orElseGet(String::new);
 *
 *     // Map the underlying data to a new type.
 *     final int hashCode = betterReturnAlt()
 *       .map(Object::hashCode)
 *       .orElse(0);
 * </pre><h2>
 *     Using {@link Result} as a try-catch wrapper
 * </h2><pre>
 *     // Standard conventions to be wrapped.
 *     public static String toWrap() throws IOException {
 *       final File f = getFile();
 *       return getContents(f);
 *     }
 * </pre><h3>
 *     Implementation
 * </h3><pre>
 *     // Generate instructions for wrapping this method.
 *     final PartialResult<String, IOException> r1 = Result.of(Name::toWrap);
 *
 *     // Consume these instructions and get a Result<T, E>.
 *     final Result<String, IOException> r2 = r1.ifErr(e -> log.warn("Oops!"));
 * </pre><p>
 *     Note that in order to use a {@link PartialResult} as a regular {@link Result},
 *     <b>you must call {@link #ifErr}</b> or another such which can consume the error.
 * </p>
 * @param <T> The type of value being wrapped.
 * @param <E> The type of error being wrapped.
 * @author PersonTheCat
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Result<T, E extends Throwable> extends OptionalResult<T, E> {

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
    static <T> Result<T, Throwable> suppress(final ThrowingSupplier<T, Throwable> attempt) {
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
     * @see Result#suppress(ThrowingSupplier)
     * @param attempt An expression which does not yield a value, but may err.
     * @return A result which may either be OK or any kind of exception.
     */
    @CheckReturnValue
    static Result<Void, Throwable> suppress(final ThrowingRunnable<Throwable> attempt) {
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
     * @return A result which may either be a value or empty.
     */
    @CheckReturnValue
    static <T, E extends Throwable> OptionalResult<T, E> nullable(final T value) {
        return value != null ? Result.ok(value) : Result.empty();
    }

    /**
     * Variant of {@link #nullable(T)} in which <code>T</code> is wrapped in {@link Optional}.
     *
     * @param value Any given value which may be empty.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which either be a value or empty.
     */
    @CheckReturnValue
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T, E extends Throwable> OptionalResult<T, E> nullable(final Optional<T> value) {
        return value.isPresent() ? Result.ok(value.get()) : Result.empty();
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
    static <T, E extends Throwable> PartialOptionalResult<T, E> nullable(final ThrowingSupplier<T, E> attempt) {
        return new PendingNullable<>(attempt);
    }

    /**
     * Variant of {@link #nullable(ThrowingSupplier)} in which the supplier returns {@link Optional}.
     *
     * @see Result#nullable(T)
     * @param attempt An expression which either yields a value or throws an error.
     * @param <T> The type of value being consumed by the wrapper.
     * @param <E> The type of error being consumed by the wrapper.
     * @return A result which may either be a value, an error, or null.
     */
    @CheckReturnValue
    static <T, E extends Throwable> PartialOptionalResult<T, E> nullable(final ThrowingOptionalSupplier<T, E> attempt) {
        return new PendingNullable<>(() -> attempt.get().orElse(null));
    }

    /**
     * Variant of {@link #suppress} which is allowed to contain null values.
     *
     * @see Result#nullable(T)
     * @param attempt An expression which either yields a value, throws <b>any</b> error,
     *                or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @return A result which may either be a value, <b>any</b> error, or null.
     */
    static <T> OptionalResult<T, Throwable> suppressNullable(final ThrowingSupplier<T, Throwable> attempt) {
        try {
            return nullable(attempt.get());
        } catch (Throwable t) {
            return err(t);
        }
    }

    /**
     * Variant of {@link #suppressNullable(ThrowingSupplier)} in which the return value is
     * wrapped in {@link Optional}.
     *
     * @see Result#suppressNullable(ThrowingSupplier)
     * @param attempt An expression which either yields a value, throws <b>any</b> error,
     *                or returns null.
     * @param <T> The type of value being consumed by the wrapper.
     * @return A result which may either be a value, <b>any</b> error, or null.
     */
    static <T> OptionalResult<T, Throwable> suppressNullable(final ThrowingOptionalSupplier<T, Throwable> attempt) {
        return suppressNullable(() -> attempt.get().orElse(null));
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
     * Creates a new {@link Protocol} for handling the specified error type. Can be
     * chained with additional definitions to handle multiple different error types,
     * as needed.
     *
     * @param clazz The class of error to be handled by this definition.
     * @param f     The function used for handling this kind of error.
     * @param <E>   The type of error being handled by this procedure.
     * @return A new {@link Protocol} for handling the specified error type.
     */
    @CheckReturnValue
    static <E extends Throwable> Protocol define(final Class<E> clazz, final Consumer<E> f) {
        return new Protocol().and(clazz, f);
    }

    /**
     * Creates a new {@link Resolver} for handling the specified error type. Can be
     * chained with additional definitions to handled multiple different error types,
     * as needed.
     *
     * @param clazz The class of error to be handled by this definition.
     * @param f     The function used for handling this kind of error.
     * @param <T>   The type of value being wrapped by this handler.
     * @param <E>   The type of error being handled by this procedure.
     * @return A new {@link Protocol} for handling the specified error type.
     */
    @CheckReturnValue
    static <T, E extends Throwable> Resolver<T> resolve(final Class<E> clazz, final Function<E, T> f) {
        return new Resolver<T>().and(clazz, f);
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
                    throw Shorthand.<E>errorFound(e);
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    Result<T, E> ifOk(final Consumer<T> f);

    /**
     * Effectively casts this object into a standard Optional instance.
     * Prefer calling {@link Result#get(Consumer)}, as this removes the need for
     * any implicit error checking.
     *
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @Override
    @CheckReturnValue
    Optional<T> get();

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    @Override
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
     * Strips away the final layer of uncertainty and returns a definite {@link Value}.
     *
     * <p>e.g.</p>
     * <pre>
     *   final String value = getStringOrFail()
     *     .resolve(e -> "Default value")
     *     .expose(); // No consequences.
     * </pre>
     *
     * @param ifErr A handler which provides a default value.
     * @return A result which can only be a {@link Value}.
     */
    @CheckReturnValue
    Value<T, E> resolve(final Function<E, T> ifErr);

    /**
     * Yields the underlying value or else the input. This is equivalent to
     * running `getResult().get(e -> {...}).orElse();`
     *
     * @return The underlying value, or else the input.
     */
    @Override
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
    @Override
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
     * Maps to a new Result if an error is present. Use this whenever your first and
     * second attempt at retrieving a value may fail.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @CheckReturnValue
    PartialResult<T, E> orElseTry(final ThrowingFunction<E, T, E> f);

    /**
     * Variant of {@link #orElseTry(ThrowingFunction)} which ignores the error value,
     * if present.
     *
     * @param f A new function to attempt in the presence of an error.
     * @return The pending result of the new function, if an error is present,
     *         or else a complete {@link Result}.
     */
    @Override
    @CheckReturnValue
    PartialResult<T, E> orElseTry(final ThrowingSupplier<T, E> f);

    /**
     * Variant of {@link #orElseTry(ThrowingFunction)} which handles the error when
     * given a specific {@link Protocol}.
     *
     * @param protocol Instructions for handling errors beyond this point.
     * @param f A new function to attempt in the presence of an error.
     * @return A result which may contain either a value or <b>any</b> error.
     */
    @CheckReturnValue
    Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingFunction<E, T, Throwable> f);

    /**
     * Variant of {@link #orElseTry(Protocol, ThrowingFunction)} which does not
     * consume the underlying error.
     *
     * @param protocol Instructions for handling errors beyond this point.
     * @param f A new function to attempt in the presence of an error.
     * @return A result which may contain either a value or <b>any</b> error.
     */
    @Override
    @CheckReturnValue
    default Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingSupplier<T, Throwable> f) {
        return this.orElseTry(protocol, e -> f.get());
    }

    /**
     * Variant of {@link #orElseTry(ThrowingFunction)} which handles the error when
     * given a {@link Resolver}.
     *
     * @param resolver Instructions for handling errors beyond this point.
     * @param f A new function to attempt in the presence of an error.
     * @return A result which can only be a value.
     */
    @CheckReturnValue
    Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingFunction<E, T, Throwable> f);

    /**
     * Variant of {@link #orElseTry(Resolver, ThrowingFunction)} which does not
     * consume the underlying error.
     *
     * @param resolver Instructions for handling errors beyond this point.
     * @param f A new function to attempt in the presence of an error.
     * @return A result which may contain either a value or <b>any</b> error.
     */
    @CheckReturnValue
    default Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingSupplier<T, Throwable> f) {
        return this.orElseTry(resolver, e -> f.get());
    }

    /**
     * Replaces the underlying value with the result of func.apply(). Use this
     * whenever you need to map a potential value to a different value.
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
    @Override
    @CheckReturnValue
    <M> Result<M, E> map(final Function<T, M> f);

    /**
     * Replaces the underlying error with the result of func.apply(). Use this
     * whenever you need to map a potential error to another error.
     *
     * @param f The mapper applied to the error.
     * @return A new Result with its error mapped.
     */
    @Override
    @CheckReturnValue
    <E2 extends Throwable> Result<T, E2> mapErr(final Function<E, E2> f);

    /**
     * Replaces the entire value with a new result, if present. Use this whenever
     * you need to map a potential value to another function which yields a Result.
     *
     * @param f A function which yields a new Result wrapper if a value is present.
     * @return The new function yielded, if a value is present, else this.
     */
    @CheckReturnValue
    <M> Result<M, E> flatMap(final ResultFunction<T, M, E> f);

    /**
     * Variant of {@link #flatMap(ResultFunction)} which yields an {@link OptionalResult}.
     *
     * @param f A function which yields a new Result wrapper if a value is present.
     * @return The new function yielded, if a value is present, else this.
     */
    @Override
    @CheckReturnValue
    <M> OptionalResult<M, E> flatMap(final OptionalResultFunction<T, M, E> f);

    /**
     * Replaces the entire value with a new result, if present. Use this whenever
     * you need to map a potential error to another function which yields a Result.
     *
     * @param f A function which yields a new Result wrapper if an error is present.
     * @return The new function yielded, if an error is present, or else a complete
     *         {@link Result}.
     */
    @CheckReturnValue
    <E2 extends Throwable> Result<T, E2> flatMapErr(final ResultFunction<E, T, E2> f);

    /**
     * Variant of {@link #flatMapErr(ResultFunction)} which yields an {@link OptionalResult}.
     *
     * @param f A function which yields a new Result wrapper if an error is present.
     * @return The new function yielded, if an error is present, or else a complete
     *         {@link Result}.
     */
    @Override
    @CheckReturnValue
    <E2 extends Throwable> OptionalResult<T, E2> flatMapErr(final OptionalResultFunction<E, T, E2> f);

    /**
     * Filters the underlying value out of the wrapper based on the given condition.
     *
     * <p>e.g.</p>
     * <pre>
     *   Result.of(() -> "Hello, world!) // Value is present
     *     .filter(() -> false) // Value is removed.
     *     .assertEmpty(); // No exception is thrown.
     * </pre>
     *
     * @param f A predicate which determines whether to keep the underlying value, if present.
     * @return A new result with no value, or else this.
     */
    @Override
    @CheckReturnValue
    OptionalResult<T, E> filter(final Predicate<T> f);

    /**
     * Filters the underlying error out of the wrapper based on the given condition.
     *
     * <p>e.g.</p>
     * <pre>
     *   Result.of(() -> throwException()) // Error is present
     *     .filterErr(() -> false) // Error is removed.
     *     .assertEmpty(); // No exception is thrown.
     * </pre>
     *
     * @param f A predicate which determines whether to keep the underlying error, if present.
     * @return A new result with no error, or else this.
     */
    @Override
    @CheckReturnValue
    OptionalResult<T, E> filterErr(final Predicate<E> f);

    /**
     * Consumes an event to run in the event of success in this wrapper.
     *
     * <p>e.g.</p>
     * <pre>
     *   Result.any(() -> "Hello, world!") // Result is OK
     *     .andThen(String::length); // becomes a Result&lt;Integer, E&gt;
     * </pre>
     *
     * @param f The event to run, if OK.
     * @param <M> The new type of value being consumed by the wrapper.
     * @return A new result containing the output of this function, or else this.
     */
    @Override
    @CheckReturnValue
    <M> Result<M, E> andThen(final Function<T, M> f);

    /**
     * Variant of {@link #andThen(Function)} which may fail. <b>Any error returned by this
     * event must be acknowledged</b>.
     *
     * <p>e.g.</p>
     * <pre>
     *   Result.any(() -> "Hello, world!) // Result is OK
     *     .andThenTry(s -> throwException()) // Result is not OK
     *     .ifErr(Result::WARN); // This problem will be logged.
     * </pre>
     *
     * @param attempt The next procedure to attempt, if OK.
     * @param <M> The new type of value being consumed by the wrapper.
     * @return A new result containing the output of this function, or else this.
     */
    @Override
    @CheckReturnValue
    <M> PartialResult<M, E> andThenTry(final ThrowingFunction<T, M, E> attempt);

    /**
     * Variant of {@link #andThenTry} which is allowed to throw <b>any</b> exception.
     * Returns a less restrictive wrapper.
     *
     * @param attempt The next procedure to attempt, if OK.
     * @param <M> The new type of value being consumed by the wrapper.
     * @return A new result containing the output of this function, or else this.
     */
    @Override
    @CheckReturnValue
    <M> Result<M, Throwable> andThenSuppress(final ThrowingFunction<T, M, Throwable> attempt);

    /**
     * Variant of {@link #andThen(Function)} which ignores the value in the wrapper.
     *
     * @param f The event to run, if OK.
     * @return A new result containing the output of this function, or else this.
     */
    @Override
    Result<Void, E> andThen(final Runnable f);

    /**
     * Variant of {@link #andThenTry(ThrowingFunction)} which ignores the value in
     * the wrapper.
     *
     * @param attempt The next procedure to attempt, if OK.
     * @return A new result containing the output of this function, or else this.
     */
    @Override
    @CheckReturnValue
    PartialResult<Void, E> andThenTry(final ThrowingRunnable<E> attempt);

    /**
     * Variant of {@link #andThenSuppress(ThrowingFunction)} which ignores the value
     * in the wrapper.
     *
     * @param attempt The next procedure to attempt, if OK.
     * @return A new result containing the output of this function, or else this.
     */
    @Override
    @CheckReturnValue
    Result<Void, Throwable> andThenSuppress(final ThrowingRunnable<Throwable> attempt);

    /**
     * Attempts to retrieve the underlying error, asserting that one must exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @Override
    @CheckReturnValue
    default E unwrapErr() {
        return this.expectErr("Attempted to unwrap a result with no error.");
    }

    /**
     * Yields the underlying error, throwing a generic exception if no error is present.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @return The underlying error.
     */
    @Override
    @CheckReturnValue
    E expectErr(final String message);

    /**
     * Formatted variant of {@link #expectErr}.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @param message The message to display in the event of an error.
     * @param args A series of interpolated arguments (replacing <code>{}</code>).
     * @return The underlying error.
     */
    @Override
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
    @Override
    default T orElseThrow() throws E {
        this.throwIfErr();
        return unwrap();
    }

    /**
     * If an error is present, it will be thrown by this method.
     *
     * @throws E The original error, if present.
     */
    @Override
    void throwIfErr() throws E;

    /**
     * An implementation of {@link Result} which is the outcome of a successful operation.
     *
     * @param <T> The type of value being wrapped
     * @param <E> A dummy parameter for the call site
     */
    class Value<T, E extends Throwable> implements PartialResult<T, E>, Result<T, E> {

        private static final Value<Void, ?> OK = new Value<>(Void.INSTANCE);
        private static final long serialVersionUID = 2L;

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
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isEmpty() {
            return false;
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
        public Value<T, E> ifErr(final Consumer<E> f) {
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
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Value<T, E> resolve(final Function<E, T> ifErr) {
            return this;
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
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public Value<T, E> defaultIfEmpty(final Supplier<T> defaultGetter) {
            return this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public Value<T, E> ifEmpty(final Runnable f) {
            return this;
        }

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public void assertEmpty() {}

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public void expectEmpty(final String message) {}

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public void expectEmptyF(final String message, final Object... args) {}

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
        public Value<T, E> orElseTry(final ThrowingFunction<E, T, E> f) {
            return this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Value<T, E> orElseTry(final ThrowingSupplier<T, E> f) {
            return this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Value<T, Throwable> orElseTry(final Protocol protocol, final ThrowingFunction<E, T, Throwable> f) {
            return (Value<T, Throwable>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Value<T, Throwable> orElseTry(final Protocol protocol, final ThrowingSupplier<T, Throwable> f) {
            return (Value<T, Throwable>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingFunction<E, T, Throwable> f) {
            return (Value<T, Throwable>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingSupplier<T, Throwable> f) {
            return (Value<T, Throwable>) this;
        }

        @Override
        public <M> Value<M, E> map(final Function<T, M> f) {
            return new Value<>(f.apply(this.value));
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <E2 extends Throwable> Value<T, E2> mapErr(final Function<E, E2> f) {
            return (Value<T, E2>) this;
        }

        @Override
        public <M> Result<M, E> flatMap(final ResultFunction<T, M, E> f) {
            return f.apply(this.value);
        }

        @Override
        public <M> OptionalResult<M, E> flatMap(final OptionalResultFunction<T, M, E> f) {
            return f.apply(this.value);
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <E2 extends Throwable> Result<T, E2> flatMapErr(final ResultFunction<E, T, E2> f) {
            return (Result<T, E2>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <E2 extends Throwable> OptionalResult<T, E2> flatMapErr(final OptionalResultFunction<E, T, E2> f) {
            return (OptionalResult<T, E2>) this;
        }

        @Override
        public OptionalResult<T, E> filter(final Predicate<T> f) {
            return f.test(this.value) ? this : Result.empty();
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public OptionalResult<T, E> filterErr(final Predicate<E> f) {
            return this;
        }

        @Override
        public <M> Value<M, E> andThen(final Function<T, M> f) {
            return Result.ok(f.apply(this.value));
        }

        @Override
        public PartialResult<Void, E> andThenTry(final ThrowingRunnable<E> attempt) {
            return Result.of(attempt);
        }

        @Override
        public <M> Result<M, Throwable> andThenSuppress(final ThrowingFunction<T, M, Throwable> attempt) {
            return Result.suppress(() -> attempt.apply(this.value));
        }

        @Override
        public Value<Void, E> andThen(final Runnable f) {
            f.run();
            return Result.ok();
        }

        @Override
        @Deprecated
        public <M> PartialResult<M, E> andThenTry(final ThrowingFunction<T, M, E> attempt) {
            return Result.of(() -> attempt.apply(this.value));
        }

        @Override
        @Deprecated
        public Result<Void, Throwable> andThenSuppress(final ThrowingRunnable<Throwable> attempt) {
            return Result.suppress(attempt);
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
    class Error<T, E extends Throwable> implements PartialResult<T, E>, Result<T, E> {

        private static final long serialVersionUID = 2L;

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
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        public boolean isEmpty() {
            return false;
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
         * @deprecated Unnecessary check.
         */
        @Override
        @Deprecated
        public boolean isErr(Class<? super E> clazz) {
            if (!clazz.isInstance(this.error)) {
                throw wrongErrorEx(this.error);
            }
            return true;
        }

        /**
         * @deprecated Always returns true.
         */
        @Override
        @Deprecated
        public boolean isAnyErr() {
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
                throw wrongErrorEx(this.error);
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
         * @deprecated Use {@link Error#expose}.
         */
        @Override
        @Deprecated
        public Optional<Throwable> getAnyErr() {
            return Optional.of(this.error);
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
        @Deprecated
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
                throw wrongErrorEx(this.error);
            }
        }

        @Override
        public Value<T, E> resolve(final Function<E, T> ifErr) {
            return Result.ok(ifErr.apply(this.error));
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
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public Error<T, E> defaultIfEmpty(final Supplier<T> defaultGetter) {
            return this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public Error<T, E> ifEmpty(final Runnable f) {
            return this;
        }

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public void assertEmpty() {}

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public void expectEmpty(final String message) {}

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public void expectEmptyF(final String message, final Object... args) {}

        @Override
        public T orElse(final T val) {
            return val;
        }

        @Override
        public T orElseGet(final Supplier<T> f) {
            return f.get();
        }

        @Override
        public T orElseGet(final Function<E, T> f) {
            try {
                return f.apply(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorEx(this.error);
            }
        }

        @Override
        public Pending<T, E> orElseTry(final ThrowingFunction<E, T, E> f) {
            try {
                return Result.of(() -> f.apply(this.error));
            } catch (ClassCastException ignored) {
                throw wrongErrorEx(this.error);
            }
        }

        @Override
        public PartialResult<T, E> orElseTry(final ThrowingSupplier<T, E> f) {
            return Result.of(f);
        }

        @Override
        public Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingFunction<E, T, Throwable> f) {
            return protocol.suppress(() -> f.apply(this.error));
        }

        @Override
        public Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingSupplier<T, Throwable> f) {
            return protocol.suppress(f);
        }

        @Override
        public Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingFunction<E, T, Throwable> f) {
            return resolver.suppress(() -> f.apply(this.error));
        }

        @Override
        public Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingSupplier<T, Throwable> f) {
            return resolver.suppress(f);
        }

        /**
         * @deprecated Needless allocation.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Error<M, E> map(final Function<T, M> f) {
            try {
                return (Error<M, E>) this;
            } catch (ClassCastException ignored) {
                throw wrongErrorEx(this.error);
            }
        }

        @Override
        public <E2 extends Throwable> Error<T, E2> mapErr(final Function<E, E2> f) {
            try {
                return new Error<>(f.apply(this.error));
            } catch (ClassCastException ignored) {
                throw wrongErrorEx(this.error);
            }
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Result<M, E> flatMap(final ResultFunction<T, M, E> f) {
            return (Result<M, E>) this;
        }

        @Override
        public <E2 extends Throwable> Result<T, E2> flatMapErr(final ResultFunction<E, T, E2> f) {
            try {
                return f.apply(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorEx(this.error);
            }
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> OptionalResult<M, E> flatMap(final OptionalResultFunction<T, M, E> f) {
            return (OptionalResult<M, E>) this;
        }

        @Override
        public <E2 extends Throwable> OptionalResult<T, E2> flatMapErr(final OptionalResultFunction<E, T, E2> f) {
            return f.apply(this.error);
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public OptionalResult<T, E> filter(final Predicate<T> f) {
            return this;
        }

        @Override
        public OptionalResult<T, E> filterErr(final Predicate<E> f) {
            return f.test(this.error) ? this : Result.empty();
        }

        /**
         * @deprecated Never runs function.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Error<M, E> andThen(final Function<T, M> f) {
            return (Error<M, E>) this;
        }

        /**
         * @deprecated Never runs function.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Error<M, E> andThenTry(final ThrowingFunction<T, M, E> attempt) {
            return (Error<M, E>) this;
        }

        /**
         * @deprecated Never runs function.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Error<M, Throwable> andThenSuppress(final ThrowingFunction<T, M, Throwable> attempt) {
            return (Error<M, Throwable>) this;
        }

        /**
         * @deprecated Never runs function.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Error<Void, E> andThen(final Runnable f) {
            return (Error<Void, E>) this;
        }

        /**
         * @deprecated Never runs function.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Error<Void, Throwable> andThenSuppress(final ThrowingRunnable<Throwable> attempt) {
            return (Error<Void, Throwable>) this;
        }

        /**
         * @deprecated Never runs function.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Error<Void, E> andThenTry(final ThrowingRunnable<E> attempt) {
            return (Error<Void, E>) this;
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

        private static final long serialVersionUID = 2L;

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
         * @deprecated Always returns false.
         */
        @Override
        @Deprecated
        @CheckReturnValue
        public boolean isAnyErr() {
            return false;
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
         * @deprecated Always runs function.
         */
        @Override
        @Deprecated
        public PartialResult<T, E> orElseTry(final ThrowingSupplier<T, E> f) {
            return Result.of(f);
        }

        /**
         * @deprecated Always runs function.
         */
        @Override
        @Deprecated
        public Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingSupplier<T, Throwable> f) {
            return protocol.suppress(f);
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Empty<M, E> map(final Function<T, M> f) {
            return (Empty<M, E>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <E2 extends Throwable> Empty<T, E2> mapErr(final Function<E, E2> f) {
            return (Empty<T, E2>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> OptionalResult<M, E> flatMap(final OptionalResultFunction<T, M, E> f) {
            return (OptionalResult<M, E>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <E2 extends Throwable> OptionalResult<T, E2> flatMapErr(final OptionalResultFunction<E, T, E2> f) {
            return (OptionalResult<T, E2>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Empty<T, E> filter(final Predicate<T> f) {
            return this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Empty<T, E> filterErr(final Predicate<E> f) {
            return this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Empty<M, E> andThen(final Function<T, M> f) {
            return (Empty<M, E>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Empty<M, E> andThenTry(final ThrowingFunction<T, M, E> attempt) {
            return (Empty<M, E>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public <M> Empty<M, Throwable> andThenSuppress(final ThrowingFunction<T, M, Throwable> attempt) {
            return (Empty<M, Throwable>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Empty<Void, E> andThen(final Runnable f) {
            return (Empty<Void, E>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Empty<Void, E> andThenTry(final ThrowingRunnable<E> attempt) {
            return (Empty<Void, E>) this;
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        @SuppressWarnings("unchecked")
        public Empty<Void, Throwable> andThenSuppress(final ThrowingRunnable<Throwable> attempt) {
            return (Empty<Void, Throwable>) this;
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
        public Optional<T> get(final Consumer<E> f) {
            return this.execute().get(f);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Optional<Throwable> getAnyErr() {
            return (Optional<Throwable>) this.execute().getErr();
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
        public Value<T, E> resolve(final Function<E, T> ifErr) {
            return this.execute().resolve(ifErr);
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

        @Override
        @CheckReturnValue
        public Result<T, Throwable> orElseTry(final Protocol protocol, final ThrowingFunction<E, T, Throwable> f) {
            return this.execute().orElseTry(protocol, f);
        }

        @Override
        @CheckReturnValue
        public Value<T, Throwable> orElseTry(final Resolver<T> resolver, final ThrowingFunction<E, T, Throwable> f) {
            return this.execute().orElseTry(resolver, f);
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
                return this.result = new Error<>(Shorthand.errorFound(e));
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
        public OptionalResult<T, E> ifErr(final Consumer<E> f) {
            return this.execute().ifErr(f);
        }

        @Override
        @CheckReturnValue
        public Optional<T> get(Consumer<E> f) {
            return this.execute().get(f);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Optional<Throwable> getAnyErr() {
            return (Optional<Throwable>) this.execute().getErr();
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
}
