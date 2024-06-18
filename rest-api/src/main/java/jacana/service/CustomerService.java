package jacana.service;

import java.util.List;
import java.util.Optional;

public interface CustomerService<T> {

    /**
     * Get a List of all objects, T, from the data source.
     * The returned List may be empty if nothing is found.
     *
     * @return
     */
    List<T> getAll();

    /**
     * Gets an object T from the data source with the corresponding
     * {guid}. If no object is found, then the returned Optional.isEmpty()
     * will be true.
     *
     * @param guid
     * @return
     */
    Optional<T> getByGuid(String guid);

    /**
     * Gets an object T from the data source with the corresponding
     * {email}. If no object is found, then the returned Optional.isEmpty()
     * will be true.
     *
     * @param email
     * @return
     */
    Optional<T> getByEmail(String email);

    /**
     * Inserts object T into the data source. If there are errors with
     * the insert, then a generic CustomerServiceException is thrown.
     *
     * Errors could result from the object being invalid, e.g. missing
     * required fields. Or the exception could be due to errors
     * communicating with the data source.
     *
     * @param t
     * @return
     * @throws CustomerServiceException
     */
    T insert(T t) throws CustomerServiceException;

    /**
     * Updates object T with {guid} in the data source with the
     * contents of the supplied new object T. If no object belonging
     * to {guid} is found, then CustomerNotFoundException is thrown.
     * Any other errors with the update will be a CustomerServiceException.
     *
     * Errors could result from the object being invalid, e.g. missing
     * required fields. Or the exception could be due to errors
     * communicating with the data source.
     *
     * @param guid
     * @param t
     * @return
     * @throws CustomerServiceException
     */
    T update(String guid, T t) throws CustomerServiceException, CustomerNotFoundException;

    /**
     * Deletes object T with {guid} from the data source. If no object
     * belonging to {guid} is found, then CustomerNotFoundException is thrown.
     * Any other errors with the delete will be a CustomerServiceException.
     *
     * @param guid
     * @return
     * @throws CustomerServiceException
     * @throws CustomerNotFoundException
     */
    T delete(String guid) throws CustomerServiceException, CustomerNotFoundException;
}
