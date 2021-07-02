package personthecat.fresult;

import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;
import personthecat.fresult.interfaces.ThrowingConsumer;
import personthecat.fresult.interfaces.ThrowingFunction;
import personthecat.fresult.interfaces.ThrowingRunnable;
import personthecat.fresult.interfaces.ThrowingSupplier;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.*;

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
 * </p><pre>{@code
 *   // Return the product of each block.
 *   public static Result<String, IOException> betterReturn() {
 *     final File f = getFile();
 *     try {
 *       return Result.ok(getContents(f));
 *     } catch (IOException e) {
 *       return Result.err(e);
 *     }
 *   }
 *
 *   // Standard conventions to be wrapped.
 *   public static String toWrap() throws IOException {
 *     final File f = getFile();
 *     return getContents(f);
 *   }
 *
 *   // Create and return a new error directly.
 *   public static Result<String, IOException> betterReturnAlt() {
 *     final File f = getFile();
 *     return testConditions()
 *       ? Result.ok(getContents(f))
 *       : Result.err(new IOException());
 *   }
 * }</pre><p>
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
 * @param <T> The type of value to be returned.
 * @param <E> The type of error to be caught.
 * @author PersonTheCat
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public abstract class Result<T, E extends Throwable> extends PartialResult<T, E> {

    /**
     * Accepts an error and ignores it, while still coercing it to its lowest type, thus
     * not allowing any unspecified error types to be ignored.
     *
     * <p>e.g.</p>
     * {@code Result.of(...).ifErr(Result::IGNORE);}
     */
    public static <E extends Throwable> void IGNORE(E e) {}

    /**
     * Accepts an error and logs it as a warning. This implementation is also type safe.
     * <p>e.g.</p>
     * {@code Result.of(...).ifErr(Result::WARN);}
     */
    public static <E extends Throwable> void WARN(E e) { warn("{}", e); }

    /**
     * Accepts an error and throws it. This implementation is also type safe.
     * <p>e.g.</p>
     * {@code Result.of(...).ifErr(Result::THROW);}
     */
    public static <E extends Throwable> void THROW(E e) { throw runEx(e); }

    /**
     * This constructor indicates that {@link Result} is effectively a sealed class type.
     * It may not be extended outside of this package.
     */
    Result() {}

    /**
     * Returns a generic result containing no value or error. Use this method whenever
     * no operation needs to occur, but a `Result` value is being returned.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Result<Void, E> ok() {
        return (Result<Void, E>) Value.OK;
    }

    /**
     * Returns a new result with a value and no errors. Use this method whenever no operation
     * needs to occur, but a value exists and must be wrapped in a <code>Result</code>.
     *
     * @param val The value being wrapped.
     */
    public static <T, E extends Throwable> Result<T, E> ok(T val) {
        return new Value<>(val);
    }

    /**
     * Returns a new result with an error and no value present. Use this method to enable
     * callers to respond functionally even though error handling has already occurred.
     *
     * @param e The error being wrapped.
     */
    public static <T, E extends Throwable> Result<T, E> err(E e) {
        return new Error<>(e);
    }

    /**
     * This is the primary method for constructing a new <code>Result</code> object using any
     * operation which will either yield a value or produce an error.
     *
     * <p>e.g.</p>
     * <pre>
     *   File f = getFile();
     *   // Get the result of calling f#mkdirs,
     *   // handling a potential SecurityException.
     *   boolean success = Result.of(f::mkdirs)
     *     .orElseGet(e -> false); // ignore SecurityExceptions, return default.
     * </pre>
     *
     * @param attempt An expression which either yields a value or throws an exception.
     */
    @CheckReturnValue
    public static <T, E extends Throwable> Pending<T, E> of(ThrowingSupplier<T, E> attempt) {
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
     * @param attempt An expression which does not yield a value, but may err.
     */
    @CheckReturnValue
    public static <E extends Throwable> Pending<Void, E> of(ThrowingRunnable<E> attempt) {
        return new Pending<>(() -> { attempt.run(); return Void.INSTANCE; });
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
     */
    @CheckReturnValue
    public static <T> Result<T, Throwable> any(ThrowingSupplier<T, Throwable> attempt) {
        return of(attempt).execute();
    }

    /**
     * Constructs a result from an operation which may throw <b>any exception</b> and not return
     * a value.
     *
     * @param attempt An expression which does not yield a value, but may err.
     */
    @CheckReturnValue
    public static Result<Void, Throwable> any(ThrowingRunnable<Throwable> attempt) {
        return of(attempt).execute();
    }

    /**
     * Constructs a new result from a known value which may or may not be null.
     *
     * <p>e.g.</p>
     * <pre>
     *   // Use this to wrap a value which may or may not be null
     *   // Call Optional#orElse or Optional#orElseGet to substitute
     *   // the value, if null.
     *   Object result = Result.nullable(potentiallyNullValue)
     *     .orElseGet(() -> new Object());
     * </pre>
     *
     * @param value Any given value which may be null
     */
    @CheckReturnValue
    public static <T, E extends Throwable> Value<Optional<T>, E> nullable(T value) {
        return new Value<>(Optional.ofNullable(value));
    }

    /**
     * Constructs a new result from an operation which may neither err or return a value.
     *
     * <p>e.g.</p>
     * <pre>
     *   // Use this to call a function which may either throw
     *   // an exception or simply return null. Call Optional#orElse
     *   // or Optional#orElseGet to substitute the value, if null.
     *   Object result = Result.nullable(ClassName::thisMayReturnNullOrFail)
     *     .expect("Error message!") // Potentially null value wrapped in Optional<T>.
     *     .orElseGet(() -> new Object());
     * </pre>
     *
     * @param attempt An expression which either yields a value or throws an error.
     */
    @CheckReturnValue
    public static <T, E extends Throwable> Pending<Optional<T>, E> nullable(ThrowingSupplier<T, E> attempt) {
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
     *   File f = getFile();
     *   // Attempt to generate a new FileWriter and
     *   // use it write lines into `f`.
     *   Result.with(() -> new FileWriter(f))
     *     .of(writer -> writer.write("Hello World!"))
     *     .expect("Tried to write. It failed.");
     * </pre>
     *
     * @param resource An expression which yields an {@link AutoCloseable} resource.
     */
    @CheckReturnValue
    public static <R extends AutoCloseable, E extends Throwable>
    WithResource<R, E> with(ThrowingSupplier<R, E> resource) {
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
     *   File f = getFile();
     *   Result.with(() -> new FileWriter(f), writer -> {
     *     writer.write("Hello World!");
     *   }).expect("Tried to write. It failed.");
     * </pre>
     *
     * @param resource An expression which yields an {@link AutoCloseable} resource.
     * @param attempt  An expression which consumes the resource and either yields a
     *                 value or throws an exception.
     */
    @CheckReturnValue
    public static <R extends AutoCloseable, T, E extends Throwable>
    Pending<T, E> with(ThrowingSupplier<R, E> resource, ThrowingFunction<R, T, E> attempt) {
        return with(resource).of(attempt);
    }

    /**
     * Shorthand method for following a {@link #with(ThrowingSupplier)} call with a
     * {@link WithResource#of} call. Use this for an alternative to
     * {@link Result#with(ThrowingSupplier, ThrowingFunction)} that does not return
     * a value.
     *
     * @param resource An expression which yields an {@link AutoCloseable} resource.
     * @param attempt  An expression which consumes the resource and does not yield
     *                 a value, but may err.
     */
    @CheckReturnValue
    public static <R extends AutoCloseable, E extends Throwable>
    Pending<Void, E> with(ThrowingSupplier<R, E> resource, ThrowingConsumer<R, E> attempt) {
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
    public static <E extends Exception> Protocol define(Class<E> clazz, Consumer<E> func) {
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
    public static <E extends Throwable> Result<Void, E> join(Result<Void, E>... results) {
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
    public static <E extends Throwable> Pending<Void, E> join(Result.Pending<Void, E>... results) {
        return new Pending<>(() -> {
            for (Result.Pending<Void, E> result : results) {
                result.orElseThrow();
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
    public static <T, E extends Throwable> Pending<List<T>, E> collect(Result<T, E>... results) {
        return new Pending<>(() -> {
            final List<T> list = new ArrayList<>();
            for (Result<T, E> result : results) {
                list.add(result.orElseThrow());
            }
            return list;
        });
    }

    /**
     * Returns whether a value was yielded or no error occurred.
     *
     * <p>e.g.</p>
     * <pre><code>
     *   Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it does not err.
     *   if (result.isOk()) {
     *       ...
     *   }
     * </code></pre>
     *
     * @return true, if a value is present.
     */
    @CheckReturnValue
    public abstract boolean isOk();

    /**
     * Accepts an expression for what to do in the event of no error
     * being present. Use this whenever you want to functionally handle
     * both code paths (i.e. error vs. value).
     *
     * @param f A function consuming the value, if present.
     * @return This, or else a complete {@link Result}.
     */
    public abstract Result<T, E> ifOk(Consumer<T> f);

    /**
     * Effectively casts this object into a standard Optional instance.
     * Prefer calling {@link Result#get(Consumer)}, as this removes the need for
     * any implicit error checking.
     *
     * @return The underlying value, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    public abstract Optional<T> get();

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    @CheckReturnValue
    public abstract Optional<E> getErr();

    /**
     * Yields the underlying value or else the input. This is equivalent to
     * running `getResult().get(e -> {...}).orElse();`
     *
     * @return The underlying value, or else the input.
     */
    @CheckReturnValue
    public abstract T orElse(T val);

    /**
     * Variant of {@link PartialResult#orElseGet(Function)} which does not specifically
     * consume the error.
     *
     * @param f A function which supplies a new value if none is present in the
     *             wrapper.
     * @return The underlying value, or else func.get().
     */
    @CheckReturnValue
    public abstract T orElseGet(Supplier<T> f);

    /**
     * Transposes a result with an optional value into an optional result.
     *
     * @throws ClassCastException if the underlying value is not wrapped in {@link Optional<U>}.
     * @return A new Result wrapping the value directly, itself wrapped in {@link Optional}.
     */
    @CheckReturnValue
    public abstract <U> Optional<Result<U, E>> transpose();

    /**
     * Replaces the underlying value with the result of func.apply().
     * Use this whenever you need to map a potential value to a
     * different value.
     *
     * <p>e.g.</p>
     * <pre>
     *   int secretArrayLength = Result.of(() -> getSecretArray())
     *      .map(array -> array.length) // Return the length instead.
     *      .get(e -> {...}) // Handle any errors.
     *      .orElse(0); // Didn't work. Call Optional#orElse().
     * </pre>
     *
     * @param f The mapper applied to the value.
     * @return A new Result with its value mapped.
     */
    @CheckReturnValue
    public abstract <M> Result<M, E> map(Function<T, M> f);

    /**
     * Replaces the underlying error with the result of func.apply().
     * Use this whenever you need to map a potential error to another
     * error.
     *
     * @param f The mapper applied to the error.
     * @return A new Result with its error mapped.
     */
    @CheckReturnValue
    public abstract <E2 extends Throwable> Result<T, E2> mapErr(Function<E, E2> f);

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
    public abstract Result<T, E> flatMap(Function<T, Result<T, E>> f);

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
    public abstract Result<T, E> flatMapErr(Function<E, Result<T, E>> f);

    /**
     * An implementation of {@link Result} which is the outcome of a successful operation.
     *
     * @param <T> The type of value being wrapped
     * @param <E> A dummy parameter for the call site
     */
    public static class Value<T, E extends Throwable> extends Result<T, E> {

        private static final Value<Void, ?> OK = new Value<>(Void.INSTANCE);

        private final T value;

        Value(T value) {
            this.value = value;
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
         * @deprecated Always false.
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
        public Result<T, E> ifErr(Consumer<E> f) {
            return this;
        }

        /**
         * @deprecated Always true.
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
        public Result<T, E> ifOk(Consumer<T> f) {
            f.accept(this.value);
            return this;
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public Optional<T> get() {
            return Optional.ofNullable(this.value);
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
         * @deprecated Always returns <code>ifOk#apply</code>
         */
        @Override
        @Deprecated
        public <U> U fold(Function<T, U> ifOk, Function<E, U> ifErr) {
            return ifOk.apply(this.value);
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T expect(String message) {
            return this.value;
        }

        /**
         * @deprecated Always fails.
         */
        @Override
        @Deprecated
        public E expectErr(String message) {
            throw unwrapEx(message);
        }

        /**
         * @deprecated Has no effect.
         */
        @Override
        @Deprecated
        public void throwIfPresent() {}

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T orElse(T val) {
            return this.value;
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T orElseGet(Supplier<T> f) {
            return this.value;
        }

        /**
         * @deprecated Call {@link Value#expose} instead.
         */
        @Override
        @Deprecated
        public T orElseGet(Function<E, T> f) {
            return this.value;
        }

        /**
         * @deprecated Nothing to try.
         */
        @Override
        @Deprecated
        public Result<T, E> orElseTry(ThrowingFunction<E, T, E> f) {
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
        public <M> Result<M, E> map(Function<T, M> f) {
            return new Value<>(f.apply(this.value));
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public <E2 extends Throwable> Result<T, E2> mapErr(Function<E, E2> f) {
            return new Value<>(this.value);
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public Result<T, E> flatMap(Function<T, Result<T, E>> f) {
            return f.apply(this.value);
        }

        /**
         * @deprecated Always returns this.
         */
        @Override
        @Deprecated
        public Result<T, E> flatMapErr(Function<E, Result<T, E>> f) {
            return this;
        }
    }

    /**
     * An implementation of {@link Result} which is the outcome of a failure.
     *
     * @param <T> A dummy parameter for the call site
     * @param <E> The type of error being wrapped
     */
    public static class Error<T, E extends Throwable> extends Result<T, E> {

        private final E error;

        Error(E error) {
            this.error = error;
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
         * @deprecated Always true.
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
        public Result<T, E> ifErr(Consumer<E> f) {
            try {
                f.accept(this.error);
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
            return this;
        }

        /**
         * @deprecated Always false.
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
        public Result<T, E> ifOk(Consumer<T> f) {
            return this;
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
            return Optional.ofNullable(this.error);
        }

        /**
         * @deprecated Always returns <code>ifErr#apply</code>.
         */
        @Override
        @Deprecated
        public <U> U fold(Function<T, U> ifOk, Function<E, U> ifErr) {
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
        public T expect(String message) {
            throw unwrapEx(message);
        }

        /**
         * @deprecated Call {@link Error#expose} instead.
         */
        @Override
        @Deprecated
        public E expectErr(String message) {
            return this.error;
        }

        @Override
        public void throwIfPresent() throws E {
            throw this.error;
        }

        /**
         * @deprecated Always returns val.
         */
        @Override
        @Deprecated
        public T orElse(T val) {
            return val;
        }

        /**
         * @deprecated Always returns val.
         */
        @Override
        @Deprecated
        public T orElseGet(Supplier<T> f) {
            return f.get();
        }

        /**
         * @deprecated Always returns
         */
        @Override
        @Deprecated
        public T orElseGet(Function<E, T> f) {
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
        public Pending<T, E> orElseTry(ThrowingFunction<E, T, E> f) {
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
        public <M> Result<M, E> map(Function<T, M> f) {
            try {
                return new Error<>(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
        }

        /** @deprecated Always returns other. */
        @Override
        @Deprecated
        public <E2 extends Throwable> Result<T, E2> mapErr(Function<E, E2> f) {
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
        public Result<T, E> flatMap(Function<T, Result<T, E>> f) {
            return this;
        }

        /**
         * @deprecated Always returns other.
         */
        @Override
        @Deprecated
        public Result<T, E> flatMapErr(Function<E, Result<T, E>> f) {
            try {
                return f.apply(this.error);
            } catch (ClassCastException ignored) {
                throw wrongErrorFound(this.error);
            }
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
     * @param <T>
     * @param <E>
     */
    public static class Pending<T, E extends Throwable> extends PartialResult<T, E> {

        private final ThrowingSupplier<T, E> getter;
        private volatile Result<T, E> result = null;

        private Pending(ThrowingSupplier<T, E> getter) {
            this.getter = getter;
        }

        /**
         * Implementation of {@link PartialResult#isErr}.
         * <p>
         *     The implementation of this method differs from that of {@link Error#isErr}. By
         *   design, the {@link Result} wrapper is only intended for handling <em>one specific
         *   type of error</em> at a time. The other implementations of this method <em>do</em>
         *   reflect this intention.
         * </p><p>
         *     Unfortunately, due to type erasure in Java, it is impossible to access the actual
         *   type of <code>E</code> at runtime. This means we cannot verify whether the expected
         *   error has been caught until an implicit cast takes place at the call site. As a
         *   result, this method is marked as <b>unsafe</b>. Consider using {@link #isAnyErr}
         *   instead if the type of error being caught is not important for a given use case.
         * </p>
         * @deprecated Unsafe due to type erasure - may contain unexpected exception.
         * @return Whether <em>any</em> error is found in the wrapper.
         */
        @Override
        @Deprecated
        @CheckReturnValue
        public boolean isErr() {
            return this.isAnyErr();
        }

        /**
         * Variant of {@link PartialResult#isErr} which can safely guarantee whether the
         * expected type of error is present in the wrapper. If no error is present, this
         * function yields <code>false</code>. If the expected error is present, this function
         * yields <code>true</code>. If an unexpected error is found, <b>it will be thrown</b>.
         *
         * @throws WrongErrorException If any other type of exception is found.
         * @param clazz The type of error expected by the wrapper.
         * @return Whether an expected type of exception is found.
         */
        @CheckReturnValue
        public boolean isErr(Class<? super E> clazz) {
            final Result<T, E> result = this.execute();
            if (result instanceof Error) {
                final E error = ((Error<T, E>) result).expose();
                if (!clazz.isInstance(error)) {
                    throw wrongErrorFound(error);
                }
                return true;
            }
            return false;
        }

        /**
         * Variant of {@link PartialResult#isErr} which provides a semantic contract that the
         * type of error being handled is not significant.
         *
         * @return Whether <em>any</em> error is found in the wrapper.
         */
        @CheckReturnValue
        public boolean isAnyErr() {
            return this.execute().isErr();
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
        public Result<T, E> ifErr(Consumer<E> f) {
            return this.execute().ifErr(f);
        }

        @Override
        @CheckReturnValue
        public <U> U fold(Function<T, U> ifOk, Function<E, U> ifErr) {
            return this.execute().fold(ifOk, ifErr);
        }

        @Override
        public T expect(String message) {
            return this.execute().expect(message);
        }

        /**
         * Implementation of {@link PartialResult#expectErr}. When calling this method, it
         * is unknown whether the type of error being wrapped is an instance of <code>E</code>.
         * For this reason, the output of this method may produce a {@link ClassCastException}
         * which cannot be handled by the wrapper.
         *
         * <p>
         *   Instead of calling this method, it may be preferable to call {@link #expectAnyErr}
         *   which will return a generic type of {@link Throwable} exception.
         * </p>
         *
         * @deprecated May not be an instance of <code>E</code>.
         * @throws ResultUnwrapException If no error is present to be unwrapped.
         * @param message The message to display in the event of an error.
         * @return The underlying error.
         */
        @Override
        @Deprecated
        public E expectErr(String message) {
            return this.execute().expectErr(message);
        }

        /**
         * @deprecated May not be an instance of <code>E</code>.
         * @see Pending#expectErr
         * @throws ResultUnwrapException If no error is present to be unwrapped.
         * @param message The message to display in the event of an error.
         * @param args A series of interpolated arguments (replacing <code>{}</code>).
         * @return The underlying error.
         */
        @Override
        @Deprecated
        public E expectErrF(String message, Object... args) {
            return super.expectErrF(message, args);
        }

        /**
         * Returns a generic type of {@link Throwable} which could be any exception.
         *
         * <p>
         *   You may wish to use this method instead of {@link Pending#expectErr} as it is
         *   guaranteed to have a valid output and will never result in a {@link ClassCastException}.
         * </p>
         *
         * @throws ResultUnwrapException If no error is present to be unwrapped.
         * @param message The message to display in the event of an error.
         * @return The underlying error.
         */
        public Throwable expectAnyErr(String message) {
            return this.execute().expectErr(message);
        }

        /**
         * @see Pending#expectAnyErr(String).
         * @throws ResultUnwrapException If no error is present to be unwrapped.
         * @param message The message to display in the event of an error.
         * @param args A series of interpolated arguments (replacing <code>{}</code>).
         * @return The underlying error.
         */
        public Throwable expectAnyErrF(String message, Object... args) {
            return this.execute().expectErrF(message, args);
        }

        @Override
        public void throwIfPresent() throws E {
            this.execute().throwIfPresent();
        }

        /**
         * This method is a variant of {@link Result#orElseGet(Supplier)} which is guaranteed to
         * be safe, as it will coerce the underlying error into the expected type at the call
         * site and handle the issue if it becomes a problem.
         *
         * @param f A function which returns an alternate value given the error, if present.
         * @return The underlying value or else the product of <code>f</code>.
         */
        @Override
        @CheckReturnValue
        public T orElseGet(Function<E, T> f) {
            return this.execute().orElseGet(f);
        }

        @Override
        @CheckReturnValue
        public PartialResult<T, E> orElseTry(ThrowingFunction<E, T, E> f) {
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
            try {
                return this.result = new Value<>(this.getter.get());
            } catch (Throwable e) {
                return this.result = new Error<>(errorFound(e));
            }
        }
    }

    /** Attempts to cast the error into the appropriate subclass. */
    @SuppressWarnings("unchecked")
    static <E extends Throwable> E errorFound(Throwable err) {
        // In some cases--e.g. when not using a method reference--the
        // actual type effectively does not get cast, as `E` is only
        // the highest possible class (Throwable) at runtime. Any
        // ClassCastException will occur when it is first retrieved,
        // i.e. Result#get.
        try {
            return (E) err;
        } catch (ClassCastException e) {
            throw wrongErrorFound(err);
        }
    }

    /** Forwards `err` and informs the user that the wrong kind of error was caught. */
    private static WrongErrorException wrongErrorFound(Throwable err) {
        return wrongErrorEx("Wrong type of error caught by wrapper.", err);
    }
}
