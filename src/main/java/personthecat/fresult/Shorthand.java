package personthecat.fresult;

import personthecat.fresult.exception.MissingProcedureException;
import personthecat.fresult.exception.ResultUnwrapException;
import personthecat.fresult.exception.WrongErrorException;

/**
 * A collection of shorthand methods intended to improve the readability
 * of {@link Result}.java.;
 */
class Shorthand {

    /**
     * Prints to the standard error print stream. Would ideally be
     * replaced with Log4j or some other logger.
     */
    static void warn(final String x, final Object... args) {
        System.err.println(f(x, args));
    }

    /** Shorthand for {@link RuntimeException()}. */
    static RuntimeException runEx(final Throwable e) {
        return new RuntimeException(e);
    }

    static RuntimeException runEx(final String s, final Throwable e) {
        return new RuntimeException(s, e);
    }

    static RuntimeException runEx(final String s) {
        return new RuntimeException(s);
    }

    /** Shorthand for {@link ResultUnwrapException()}. */
    static ResultUnwrapException unwrapEx(final String msg) {
        return new ResultUnwrapException(msg);
    }

    /** Shorthand for {@link WrongErrorException()}. */
    static WrongErrorException wrongErrorEx(final Throwable e) {
        return new WrongErrorException("Wrong type of error caught by wrapper.", e);
    }

    /** Shorthand for {@link MissingProcedureException()}. */
    static MissingProcedureException missingProcedureEx(final Throwable e) {
        return new MissingProcedureException(e);
    }

    /** A neater way to interpolate strings. */
    static String f(String s, Object... args) {
        int begin = 0, si = 0, oi = 0;
        StringBuilder sb = new StringBuilder();
        while (true) {
            si = s.indexOf("{}", si);
            if (si >= 0) {
                sb.append(s, begin, si);
                sb.append(args[oi++]);
                begin = si = si + 2;
            } else {
                break;
            }
        }
        sb.append(s.substring(begin));
        return sb.toString();
    }

    /**
     * Determines whether this wrapper contains the expected type of error.
     *
     * @throws WrongErrorException if the result contains an unexpected error.
     */
    @SuppressWarnings("rawtypes")
    static <T, E extends Throwable> boolean checkError(final BasicResult<T, E> result, final Class<? super E> clazz) {
        if (result instanceof Result.Error) {
            final Throwable error = ((Result.Error) result).expose();
            if (!clazz.isInstance(error)) {
                throw wrongErrorEx(error);
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
            throw wrongErrorEx(err);
        }
    }
}