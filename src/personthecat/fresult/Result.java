package personthecat.fresult;

import personthecat.fresult.interfaces.*;
import personthecat.fresult.exception.*;

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
public class Result<T, E extends Throwable> {
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
     * The lazily-initialized result of the supplied operation. This value must be assigned
     * by the constructor, but is effectively null until a method for handling any potential
     * errors has been implemented.
     */
    private final Lazy<Value<T, E>> value;

    /**
     * Constructs a new result with a raw, un-calculated value. This is an internal
     * method which typically should not be needed by external libraries. Use
     * {@link #of} to quietly generate a new value supplier.
     *
     * @param attempt Supplies a raw value holder object.
     */
    protected Result(Supplier<Value<T, E>> attempt) {
        value = new Lazy<>(attempt);
    }

    /**
     * Variant of the primary constructor, used when a Value object has already been
     * created. This is an internal method which typically should not be needed by
     * external libraries. Use {@link #ok} or {@link #err} for manual instantiation.
     *
     * @param value The raw value holder, containing either a value or error.
     */
    protected Result(Value<T, E> value) {
        this.value = new Lazy<>(value);
    }

    /**
     * Returns a generic result containing no value or error. Use this method whenever
     * no operation needs to occur, but a `Result` value is being returned.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Result<Void, E> ok() {
        return (Result<Void, E>) Handled.OK;
    }

    /**
     * Returns a new result with a non-null value and no errors. Use this method whenever
     * no operation needs to occur, but a value exists and must be wrapped in a `Result`.
     *
     * @param val The value being wrapped.
     */
    public static <T, E extends Throwable> Result<T, E> ok(T val) {
        return new Handled<>(Value.ok(val));
    }

    /**
     * Returns a new result with an error and no value present. Use this method to enable
     * callers to respond functionally even though error handling has already occurred.
     *
     * @param e The error being wrapped.
     */
    public static <T, E extends Throwable> Result<T, E> err(E e) {
        return new Handled<>(Value.err(e));
    }

    /**
     * This is the primary method for constructing a new `Result` object using any operation
     * which will either yield a value or produce an error.
     *
     * e.g.
     * <pre><code>
     *   File f = getFile();
     *   // Get the result of calling f#mkdirs,
     *   // handling a potential SecurityException.
     *   boolean success = Result.of(f::mkdirs)
     *     .orElseGet(e -> false); // ignore SecurityExceptions, return default.
     * </code></pre>
     *
     * @param attempt An expression which either yields a value or throws an exception.
     */
    public static <T, E extends Throwable> Result<T, E> of(ThrowingSupplier<T, E> attempt) {
        return new Result<>(() -> getValue(attempt));
    }

    /**
     * Constructs a new result from an operation which will not yield a value.
     *
     * e.g.
     * <pre><code>
     *   // Functional equivalent to a standard try-catch block.
     *   Result.of(ClassName::thisWillFail)
     *     .ifErr(e -> {...}); // Process executes here.
     * </code></pre>
     *
     * @param attempt An expression which does not yield a value, but may err.
     */
    public static <E extends Throwable> Result<Void, E> of(ThrowingRunnable<E> attempt) {
        return new Result<>(() -> getValue(attempt));
    }

    /**
     * Constructs a new result from an operation which may neither err or return a value.
     *
     * e.g.
     * <pre><code>
     *   // Use this to call a function which may either throw
     *   // an exception or simply return null. Call Optional#orElse
     *   // or Optional#orElseGet to substitute the value, if null.
     *   Object result = Result.nullable(ClassName::thisMayReturnNullOrFail)
     *     .expect("Error message!") // Potentially null value wrapped in Optional<T>.
     *     .orElseGet(() -> new Object());
     * </code></pre>
     *
     * @param attempt An expression which either yields a value or throws an error.
     */
    public static <T, E extends Throwable> Result<Optional<T>, E> nullable(ThrowingSupplier<T, E> attempt) {
        return new Result<>(() -> getValue(() -> Optional.ofNullable(attempt.get())));
    }

    /**
     * Constructs a handler for generating a new Result from an operation with resources.
     * The implementation returned, {@link WithResource}, allows one additional `with`
     * clause, enabling multiple resources to be chained, consumed, and closed. This is
     * the functional equivalent of using a standard try-with-resources block.
     *
     * e.g.
     * <pre><code>
     *   File f = getFile();
     *   // Attempt to generate a new FileWriter and
     *   // use it write lines into `f`.
     *   Result.with(() -> new FileWriter(f))
     *     .of(writer -> writer.write("Hello World!"))
     *     .expect("Tried to write. It failed.");
     * </code></pre>
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
     * <pre><code>
     *   File f = getFile();
     *   Result.with(() -> new FileWriter(f), writer -> {
     *     writer.write("Hello World!");
     *   }).expect("Tried to write. It failed.");
     * </code></pre>
     *
     * @param resource An expression which yields an {@link AutoCloseable} resource.
     * @param attempt  An expression which consumes the resource and either yields a
     *                 value or throws an exception.
     */
    public static <R extends AutoCloseable, T, E extends Throwable>
    Result<T, E> with(ThrowingSupplier<R, E> resource, ThrowingFunction<R, T, E> attempt) {
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
    Result<Void, E> with(ThrowingSupplier<R, E> resource, ThrowingConsumer<R, E> attempt) {
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
    public static <E extends Throwable> Result<Void, E> join(Result<Void, E>... results) {
        return of(() -> {
            for (Result<Void, E> result : results) {
                result.throwIfErr();
            }
        });
    }

    /**
     * Returns whether an error occurred in the process.
     *
     * e.g.
     * <pre><code>
     *   Result<Void, RuntimeException> result = getResult();
     *   // Compute the result and proceed only if it errs.
     *   if (result.isErr()) {
     *       ...
     *   }
     * </code></pre>
     *
     * @return true, if an error is present.
     */
    public boolean isErr() {
        return getErr().isPresent();
    }

    /**
     * Accepts an expression for what to do in the event of an error
     * being present. Use this whenever you want to functionally handle
     * both code paths (i.e. error vs. value).
     *
     * @param func A function consuming the error, if present.
     * @return {@link Handled}
     */
    public Result<T, E> ifErr(Consumer<E> func) {
        try {
            getErr().ifPresent(func);
        } catch (ClassCastException e) {
            // An error is known to be present because a cast
            // was attempted on it.
            throw wrongErrorFound(unwrapErr());
        }
        return handled();
    }

    /**
     * Returns whether a value was yielded or no error occurred.
     *
     * e.g.
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
    public boolean isOk() {
        return getVal().isPresent();
    }

    /**
     * Accepts an expression for what to do in the event of no error
     * being present. Use this whenever you want to functionally handle
     * both code paths (i.e. error vs. value).
     *
     * @param func A function consuming the value, if present.
     * @return {@link Handled} if no error is present, else this.
     */
    public Result<T, E> ifOk(Consumer<T> func) {
        getVal().ifPresent(func);
        // ok ? handled by default : type check needed.
        return isOk() ? handled() : this;
    }

    /**
     * Accepts an expression for handling the expected error, if present.
     * Use this whenever you want to get the error, but still have
     * access to the Result wrapper.
     *
     * e.g.
     * <pre><code>
     *   Result.of(() -> getValueOrFail())
     *     .get(e -> {...})
     *     .ifPresent(t -> {...});
     * </code></pre>
     */
    public Optional<T> get(Consumer<E> func) {
        return ifErr(func).getVal();
    }

    /**
     * Effectively casts this object into a standard Optional instance.
     * Prefer calling {@link Result#get(Consumer)}, as this removes the need for
     * any implicit error checking.
     *
     * @throws ResultUncheckedException if an error exists and its type is unknown.
     * @return The underlying value, wrapped in {@link Optional}.
     */
    public Optional<T> get() {
        errorCheck();
        return getVal();
    }

    /**
     * Private shorthand for retrieving the wrapped value. Implementors
     * should default to {@link Result#get(Consumer)}.
     */
    private Optional<T> getVal() {
        return value.get().result;
    }

    /**
     * Retrieves the underlying error, wrapped in Optional.
     *
     * @return The underlying error, wrapped in {@link Optional}.
     */
    public Optional<E> getErr() {
        return value.get().err;
    }

    /**
     * Attempts to retrieve the underlying value, asserting that one must
     * exist.
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    public T unwrap() {
        return expect("Attempted to unwrap a result with no value.");
    }

    /**
     * Attempts to retrieve the underlying error, asserting that one must
     * exist.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    public E unwrapErr() {
        return expectErr("Attempted to unwrap a result with no error.");
    }

    /**
     * Yields the underlying value, throwing a convenient, generic exception,
     * if an error occurs.
     *
     * e.g.
     * <pre><code>
     *   // Runs an unsafe process, wrapping any original errors.
     *   Object result = getResult()
     *     .expect("Unable to get value from result.");
     * </code></pre>
     *
     * @throws ResultUnwrapException Wraps the underlying error, if present.
     * @return The underlying value.
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public T expect(String message) {
        return ifErr(e -> { throw unwrapEx(message, e); }).getVal().get();
    }

    /** Formatted variant of {@link #expect}. */
    public T expectF(String message, Object... args) {
        return expect(f(message, args));
    }

    /**
     * Yields the underlying error, throwing a generic exception if no error
     * is present.
     *
     * @throws ResultUnwrapException If no error is present to be unwrapped.
     * @return The underlying error.
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public E expectErr(String message) {
        return ifOk(t -> { throw unwrapEx(message); }).getErr().get();
    }

    /** Formatted variant of {@link #expectErr}. */
    public E expectErrF(String message, Object... args) {
        return expectErr(f(message, args));
    }

    /**
     * Variant of {@link #unwrap} which throws the original error, if applicable.
     *
     * @throws E The original error, if present.
     * @return The underlying value.
     */
    public T throwIfErr() throws E {
        throwIfPresent(getErr());
        return unwrap();
    }

    /**
     * Replaces the underlying value with the result of func.apply().
     * Use this whenever you need to map a potential value to a
     * different value.
     *
     * e.g.
     * <pre><code>
     *   int secretArrayLength = Result.of(() -> getSecretArray())
     *      .map(array -> array.length) // Return the length instead.
     *      .get(e -> {...}) // Handle any errors.
     *      .orElse(0); // Didn't work. Call Optional#orElse().
     * </code></pre>
     *
     * @param func The mapper applied to the value.
     * @return A new Result with its value mapped.
     */
    public <M> Result<M, E> map(Function<T, M> func) {
        final Optional<M> mapped = getVal().map(func);
        final Optional<E> err = getErr();
        return new Result<>(new Value<>(mapped, err));
    }

    /**
     * Replaces the underlying error with the result of func.apply().
     * Use this whenever you need to map a potential error to another
     * error.
     *
     * @param func The mapper applied to the error.
     * @return A new Result with its error mapped.
     */
    public <E2 extends Throwable> Result<T, E2> mapErr(Function<E, E2> func) {
        final Optional<T> val = getVal();
        final Optional<E2> mapped = getErr().map(func);
        return new Handled<>(new Value<>(val, mapped));
    }

    /**
     * Maps to a new Result if an error is present. Use this whenever
     * your first and second attempt at retrieving a value may fail.
     *
     * @param func A new function to attempt in the presence of an error.
     * @return The result of the new function, if an error is present,
     *         else {@link Handled}.
     */
    public Result<T, E> orElseTry(ThrowingFunction<E, T, E> func) {
        return flatMapErr(e -> Result.of((() -> func.apply(e))));
    }

    /**
     * Replaces the entire value with a new result, if present.
     * Use this whenever you need to map a potential value to
     * another function which yields a Result.
     *
     * @param func A function which yields a new Result wrapper
     *             if a value is present.
     * @return The new function yielded, if a value is present,
     *         else this.
     */
    public Result<T, E> flatMap(Function<T, Result<T, E>> func) {
        // ok ? no error to check -> new Result : error not handled -> this
        return getVal().map(func).orElse(this);
    }

    /**
     * Replaces the entire value with a new result, if present.
     * Use this whenever you need to map a potential error to
     * another function which yields a Result.
     *
     * @param func A function which yields a new Result wrapper
     *             if an error is present.
     * @return The new function yielded, if an error is present,
     *         else {@link Handled}.
     */
    public Result<T, E> flatMapErr(Function<E, Result<T, E>> func) {
        // Error ? new result : handled by default
        return getErr().map(func).orElse(handled());
    }

    /**
     * Yields the underlying value or else the input. This is equivalent to
     * running `getResult().get(e -> {...}).orElse();`
     *
     * @throws ResultUncheckedException if an error exists and its type is unknown.
     * @return The underlying value, or else the input.
     */
    public T orElse(T val) {
        // Just because an alt is supplied doesn't mean we should
        // ignore *any* possible errors that get thrown.
        errorCheck();
        return getVal().orElse(val);
    }

    /**
     * Variant of {@link #get()} which simultaneously handles a potential error
     * and yields a substitute value. Equivalent to following a {@link #get()}
     * call with a call to {@link Optional#orElseGet}.
     *
     * @throws RuntimeException wrapping the original error, if an unexpected type
     *                          of error is present in the wrapper.
     * @param func A function which returns an alternate value given the error, if
     *             present.
     * @return The underlying value, or else func.apply(E).
     */
    public T orElseGet(Function<E, T> func) {
        try {
            // Error ? map to new value : value must exist
            return getErr().map(func).orElse(unwrap());
        } catch (ClassCastException e) {
            // An error is known to be present because a cast
            // was attempted on it.
            throw wrongErrorFound(unwrapErr());
        }
    }

    /**
     * Variant of {@link #orElseGet(Function)} which does not specifically
     * consume the error.
     *
     * @throws ResultUncheckedException if an error exists and its type is unknown.
     * @param func A function which supplies a new value if none is present in the
     *             wrapper.
     * @return The underlying value, or else func.get().
     */
    public T orElseGet(Supplier<T> func) {
        errorCheck(); // Never return a value without checking the error's type.
        return isOk() ? unwrap() : func.get();
    }

    /**
     * Transposes a result with an optional value into an optional result.
     *
     * @throws ClassCastException if the underlying value is not wrapped in {@link Optional<U>}.
     * @return A new Result wrapping the value directly, itself wrapped in {@link Optional}.
     */
    @SuppressWarnings("unchecked")
    public <U> Optional<Result<U, E>> transpose() {
        if (isErr()) {
            return full(Result.err(unwrapErr()));
        }
        try {
            return ((Optional<U>) unwrap()).map(Result::ok);
        } catch (ClassCastException e) {
            throw new ClassCastException("Underlying value not wrapped in Optional<U>.");
        }
    }

    /**
     * This function is used to determine whether an error has been handled
     * before a value is retrieved. If an error does exist and has not been
     * handled, it is thrown and reported as unhandled.
     */
    protected void errorCheck() {
        if (isErr()) {
            throw uncheckedExF("Unhandled error in wrapper: {}", unwrapErr());
        }
    }

    /**
     * Called whenever the state of the error has been handled to return a new
     * {@link Handled}.
     */
    private Handled<T, E> handled() {
        return new Handled<>(value.get());
    }

    /** Runs the underlying process and converts it into a {@link Value}. */
    private static <T, E extends Throwable> Value<T, E> getValue(ThrowingSupplier<T, E> attempt) {
        try {
            return Value.ok(attempt.get());
        } catch (Throwable e) {
            return Value.err(errorFound(e));
        }
    }

    /** Variant of {@link #getValue(ThrowingSupplier)} which never yields a value. */
    private static <E extends Throwable> Value<Void, E> getValue(ThrowingRunnable<E> attempt) {
        return getValue(() -> {
            attempt.run();
            return Void.INSTANCE;
        });
    }

    /** Attempts to cast the error into the appropriate subclass. */
    @SuppressWarnings("unchecked")
    protected static <E extends Throwable> E errorFound(Throwable err) {
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
    private static RuntimeException wrongErrorFound(Throwable err) {
        return wrongErrorEx("Wrong type of error caught by wrapper.", err);
    }

    /** Throws an optional error, if present. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static <E extends Throwable> void throwIfPresent(Optional<E> err) throws E {
        if (err.isPresent()) {
            throw err.get();
        }
    }

    /**
     * An extension of the Result wrapper which aims to guarantee that potential errors
     * have already been handled. Thus, no unhandled error nag is necessary.
     */
    public static class Handled<T, E extends Throwable> extends Result<T, E> {
        /**
         * A singleton Result object used to avoid unnecessary instantiation
         * for Results that yield no value and produce no errors.
         */
        private static final Handled<Void, ?> OK = new Handled<>(Value.ok());

        /**
         * Constructs a new Result object after an error has been handled, removing some
         * safety checks and keeping various convenience functions intact.
         */
        Handled(Value<T, E> result) {
            super(result);
        }

        /** Because this wrapper has been handled, there is no need for error checking. */
        @Override protected void errorCheck() {/* Do nothing. */}
    }
}
