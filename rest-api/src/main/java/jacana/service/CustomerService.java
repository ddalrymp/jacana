package jacana.dao;

import java.util.List;
import java.util.Optional;

public interface CustomerService<T> {

    List<T> getAll();

    Optional<T> getByGuid(String guid);

    Optional<T> getByEmail(String email);

    T insert(T t) throws CustomerServiceException;

    T update(String guid, T t) throws CustomerServiceException;

    T delete(String guid) throws CustomerServiceException;
}
