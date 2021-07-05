package personthecat.fresult.exception;

/** Called whenever no procedure is defined for handling an error. */
public class MissingProcedureException extends RuntimeException {
    public MissingProcedureException(final Throwable e) {
        super(e);
    }
}