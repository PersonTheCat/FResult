package personthecat.fresult.exception;

/**
 * An error thrown whenever an unexpected type of exception is
 * caught by a Result wrapper.
 */
public class WrongErrorException extends RuntimeException {
    public WrongErrorException(final String msg, final Throwable e) {
        super(msg, e);
    }
}