package personthecat.fresult;

import java.util.Optional;
import java.util.function.Supplier;

import static personthecat.fresult.Shorthand.*;

/**
 * Creates a sort of lazily initialized value. Values wrapped in
 * this class will not exist until the first time they are used.
 */
public class Lazy<T> {

    /** The underlying value being wrapped by this object. */
    private T value = null;
    /** A supplier used for producing the value when it is ready. */
    private final Supplier<T> supplier;

    /** The primary constructor with instructions for producing the value. */
    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /** To be used in the event that a value already exists. */
    public Lazy(T value) {
        this.value = value;
        supplier = () -> null;
    }

    /** The primary method for retrieving the underlying value. */
    public T get() {
        if (value == null) {
            value = supplier.get();
            if (value == null) {
                throw new NullPointerException("Lazy value produced nothing.");
            }
        }
        return value;
    }

    /** Returns the value only if it has already been computed. */
    public Optional<T> getIfComputed() {
        return nullable(value);
    }

    /** Returns whether the underlying operation has completed. */
    public boolean computed() {
        return value != null;
    }
}