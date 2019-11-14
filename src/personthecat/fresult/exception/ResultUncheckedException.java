package personthecat.fresult.exception;

/**
 * Called whenever an unknown type of error is present in a Result
 * wrapper.
 */
public class ResultUncheckedException extends RuntimeException {
    public ResultUncheckedException(String msg) {
        super(msg);
    }
}