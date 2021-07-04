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
    static void warn(String x, Object... args) {
        System.err.println(f(x, args));
    }

    /** Shorthand for {@link RuntimeException()}. */
    static RuntimeException runEx(Throwable e) {
        return new RuntimeException(e);
    }

    static RuntimeException runEx(String s, Throwable e) {
        return new RuntimeException(s, e);
    }

    static RuntimeException runEx(String s) {
        return new RuntimeException(s);
    }

    /** Shorthand for {@link ResultUnwrapException()}. */
    static ResultUnwrapException unwrapEx(String msg) {
        return new ResultUnwrapException(msg);
    }

    /** Shorthand for {@link WrongErrorException()}. */
    static WrongErrorException wrongErrorEx(String msg, Throwable e) {
        return new WrongErrorException(msg, e);
    }

    /** Shorthand for {@link MissingProcedureException()}. */
    static MissingProcedureException missingProcedureEx(Throwable e) {
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
}