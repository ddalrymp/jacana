package jacana.service;

public class CustomerServiceException extends Exception {
    public CustomerServiceException(String errorMessage) {
        super(errorMessage);
    }
}
