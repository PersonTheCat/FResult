package personthecat.fresult.exception;

/**
 * Thrown whenever an unwrap is attempted on a Result despite no value
 * being present.
 */
public class ResultUnwrapException extends RuntimeException {
    public ResultUnwrapException(final String msg) {
        super(msg);
    }
}
