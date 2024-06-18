package jacana.rest;

/**
 * Wrapper to produce an HTTP Response object when the underlying
 * layer throws an Exception.
 */
public class ExceptionMessage {

    Exception exception;

    public ExceptionMessage(Exception exception) {
        this.exception = exception;
    }

    public String getMessage() {
        return exception.getMessage();
    }
}
