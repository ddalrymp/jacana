package jacana.dao;

public class CustomerServiceException extends Exception {
    public CustomerServiceException(String errorMessage) {
        super(errorMessage);
    }
    public CustomerServiceException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
