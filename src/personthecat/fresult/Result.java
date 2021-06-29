package personthecat.fresult;

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
     * e.g. {@code Result.of(...).ifErr(Result::IGNORE);}
     */
    public static <E extends Throwable> void IGNORE(E e) {}

    /**
     * Accepts an error and logs it as a warning. This implementation is also type safe.
     * e.g. {@code Result.of(...).ifErr(Result::WARN);}
     */
    public static <E extends Throwable> void WARN(E e) { warn("{}", e); }

    /**
     * Accepts an error and throws it. This implementation is also type safe.
     * e.g. {@code Result.of(...).ifErr(Result::THROW);}
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
     * e.g.
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
     * e.g.
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
     * Constructs a new result from a known value which may or may not be null.
     *
     * e.g.
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
    public static <T, E extends Throwable> Value<Optional<T>, E> nullable(T value) {
        return new Value<>(Optional.ofNullable(value));
    }

    /**
     * Constructs a new result from an operation which may neither err or return a value.
     *
     * e.g.
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
    public static <T, E extends Throwable> Pending<Optional<T>, E> nullable(ThrowingSupplier<T, E> attempt) {
        return new Pending<>(() -> Optional.ofNullable(attempt.get()));
    }

    /**
     * Constructs a handler for generating a new Result from an operation with resources.
     * The implementation returned, {@link WithResource}, allows one additional `with`
     * clause, enabling multiple resources to be chained, consumed, and closed. This is
     * the functional equivalent of using a standard try-with-resources block.
     *
     * e.g.
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
     * e.g.
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
    public static <E extends Throwable> Pending<Void, E> join(Result<Void, E>... results) {
        return new Pending<>(() -> {
            for (Result<Void, E> result : results) {
                result.orElseThrow();
            }
            return Void.INSTANCE;
        });
    }
    /**
     * Wraps any exceptions from the array into a new Result object.
     *
     * @param results An array of result wrappers to be joined.
     * @return A new wrapper which yields no value, but throws any possible exception from
     *         the input.
     */
    @SafeVarargs
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
     * Effectively casts this object into a standard Optional instance.
     * Prefer calling {@link Result#get(Consumer)}, as this removes the need for
     * any implicit error checking.
     *
     * @return The underlying value, wrapped in {@link Optional}.
     */
    public abstract Optional<T> get();

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    public abstract Optional<E> getErr();

    /**
     * Yields the underlying value or else the input. This is equivalent to
     * running `getResult().get(e -> {...}).orElse();`
     *
     * @return The underlying value, or else the input.
     */
    public abstract T orElse(T val);

    /**
     * Variant of {@link PartialResult#orElseGet(Function)} which does not specifically
     * consume the error.
     *
     * @param f A function which supplies a new value if none is present in the
     *             wrapper.
     * @return The underlying value, or else func.get().
     */
    public abstract T orElseGet(Supplier<T> f);

    /**
     * Transposes a result with an optional value into an optional result.
     *
     * @throws ClassCastException if the underlying value is not wrapped in {@link Optional<U>}.
     * @return A new Result wrapping the value directly, itself wrapped in {@link Optional}.
     */
    public abstract <U> Optional<Result<U, E>> transpose();

    /**
     * Replaces the underlying value with the result of func.apply().
     * Use this whenever you need to map a potential value to a
     * different value.
     *
     * e.g.
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
    public abstract <M> Result<M, E> map(Function<T, M> f);

    /**
     * Replaces the underlying error with the result of func.apply().
     * Use this whenever you need to map a potential error to another
     * error.
     *
     * @param f The mapper applied to the error.
     * @return A new Result with its error mapped.
     */
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

        @Override
        public boolean isErr() {
            return false;
        }

        @Override
        public Result<T, E> ifErr(Consumer<E> f) {
            return this;
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public Result<T, E> ifOk(Consumer<T> f) {
            f.accept(this.value);
            return this;
        }

        @Override
        public Optional<T> get() {
            return Optional.ofNullable(this.value);
        }

        @Override
        public Optional<E> getErr() {
            return Optional.empty();
        }

        @Override
        public T expect(String message) {
            return this.value;
        }

        @Override
        public E expectErr(String message) {
            throw unwrapEx(message);
        }

        @Override
        public void throwIfPresent() {}

        @Override
        public T orElse(T val) {
            return this.value;
        }

        @Override
        public T orElseGet(Supplier<T> f) {
            return this.value;
        }

        @Override
        public T orElseGet(Function<E, T> f) {
            return this.value;
        }

        @Override
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

        @Override
        public <M> Result<M, E> map(Function<T, M> f) {
            return new Value<>(f.apply(this.value));
        }

        @Override
        public <E2 extends Throwable> Result<T, E2> mapErr(Function<E, E2> f) {
            return new Value<>(this.value);
        }

        @Override
        public Result<T, E> flatMap(Function<T, Result<T, E>> f) {
            return f.apply(this.value);
        }

        @Override
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

        @Override
        public boolean isErr() {
            return true;
        }

        @Override
        public Result<T, E> ifErr(Consumer<E> f) {
            try {
                f.accept(this.error);
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
            return this;
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public Result<T, E> ifOk(Consumer<T> f) {
            return this;
        }

        @Override
        public Optional<T> get() {
            return Optional.empty();
        }

        @Override
        public Optional<E> getErr() {
            try {
                return Optional.ofNullable(this.error);
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public T expect(String message) {
            throw unwrapEx(message);
        }

        @Override
        public E expectErr(String message) {
            try {
                return this.error;
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public void throwIfPresent() throws E {
            try {
                throw this.error;
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public T orElse(T val) {
            return val;
        }

        @Override
        public T orElseGet(Supplier<T> f) {
            return f.get();
        }

        @Override
        public T orElseGet(Function<E, T> f) {
            try {
                return f.apply(this.error);
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public Pending<T, E> orElseTry(ThrowingFunction<E, T, E> f) {
            try {
                return Result.of(() -> f.apply(this.error));
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public <U> Optional<Result<U, E>> transpose() {
            try {
                return Optional.of(new Error<>(this.error));
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public <M> Result<M, E> map(Function<T, M> f) {
            try {
                return new Error<>(this.error);
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public <E2 extends Throwable> Result<T, E2> mapErr(Function<E, E2> f) {
            try {
                return new Error<>(f.apply(this.error));
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
            }
        }

        @Override
        public Result<T, E> flatMap(Function<T, Result<T, E>> f) {
            return this;
        }

        @Override
        public Result<T, E> flatMapErr(Function<E, Result<T, E>> f) {
            try {
                return f.apply(this.error);
            } catch (ClassCastException ignored) {
                throw Result.wrongErrorFound(this.error);
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

        @Override
        public boolean isErr() {
            return this.execute().isErr();
        }

        @Override
        public Result<T, E> ifErr(Consumer<E> f) {
            return this.execute().ifErr(f);
        }

        @Override
        public boolean isOk() {
            return this.execute().isOk();
        }

        @Override
        public Result<T, E> ifOk(Consumer<T> f) {
            return this.execute().ifOk(f);
        }

        @Override
        public T expect(String message) {
            return this.execute().expect(message);
        }

        @Override
        public E expectErr(String message) {
            return this.execute().expectErr(message);
        }

        @Override
        public void throwIfPresent() throws E {
            this.execute().throwIfPresent();
        }

        @Override
        public T orElseGet(Function<E, T> f) {
            return this.execute().orElseGet(f);
        }

        @Override
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
        public synchronized Result<T, E> execute() {
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
