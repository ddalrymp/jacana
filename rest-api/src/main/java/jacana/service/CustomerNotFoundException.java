package jacana.service;

public class CustomerNotFoundException extends CustomerServiceException {
    public CustomerNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
