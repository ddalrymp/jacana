package jacana.rest;

import jacana.service.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.metrics.Counter;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Customer management interface
 *
 * getAll
 * curl -X GET http://localhost:8080/customers
 *
 * getByGuid by email
 * curl -X GET http://localhost:8080/customers?email={email}
 *
 * getByGuid by guid
 * curl -X GET http://localhost:8080/customers?guid={guid}
 *
 * create Customer
 * curl -X POST -H "Content-Type: application/json" -d '{"email":"foo@example.com"}' http://localhost:8080/customers
 *
 * update Customer
 * curl -X PUT -H "Content-Type: application/json" -d '{"email":"foo@example.com"}' http://localhost:8080/customers/{guid}
 *
 * delete Customer
 * curl -X DELETE -H "Content-Type: application/json" http://localhost:8080/customers/{guid}
 *
 * The full body of the Customer object is
 * {
 *     "guid": "{UUID}",
 *     "namePrefix": {name prefix, e.g. Mr, Mrs},
 *     "nameSurname": {surname, e.g. first name},
 *     "nameMiddle": {middle name},
 *     "nameFamily": {family name, e.g. last name},
 *     "nameSuffix": {name suffix, e.g. Ph.D.},
 *     "email": "{valid email address}",
 *     "phone": "{phone number}"
 * }
 */
@ApplicationScoped
@Path("/customers")
public class CustomerResource {

    private static final Logger LOGGER = Logger.getLogger(CustomerResource.class.getName());

    private final CustomerServiceMySQL customerMySQL;

    @Inject
    public CustomerResource(CustomerServiceMySQL customerMySQL) {
        this.customerMySQL = customerMySQL;
    }

    private static final String INSERT_COUNTER_NAME = "insertCustomer";
    private static final String INSERT_COUNTER_DESCRIPTION = "Counts insert Customer operations";
    private static final String INSERT_TIMER_NAME = "insertCustomerTimer";
    private static final String INSERT_TIMER_DESCRIPTION = "Times all insert Customer operations";
    private static final String UPDATE_COUNTER_NAME = "updateCustomer";
    private static final String UPDATE_COUNTER_DESCRIPTION = "Counts update Customer operations";
    private static final String UPDATE_TIMER_NAME = "updateCustomerTimer";
    private static final String UPDATE_TIMER_DESCRIPTION = "Times all update Customer operations";
    private static final String DELETE_COUNTER_NAME = "deleteCustomer";
    private static final String DELETE_COUNTER_DESCRIPTION = "Counts delete Customer operations";
    private static final String DELETE_TIMER_NAME = "deleteCustomerTimer";
    private static final String DELETE_TIMER_DESCRIPTION = "Times all delete Customer operations";

    @Inject
    @Metric(name = "insertCustomerErrors", absolute = true)
    private Counter insertCustomerErrors;
    @Inject
    @Metric(name = "updateCustomerErrors", absolute = true)
    private Counter updateCustomerErrors;
    @Inject
    @Metric(name = "deleteCustomerErrors", absolute = true)
    private Counter deleteCustomerErrors;

    /**
     * Returns a JSON array of all customers in the database with
     * support for optional filtering by either a specific
     * Customer guid or Customer email. If no Customers are in the
     * database or if no Customer is found with a matching guid or
     * email, then an empty JSON array is returned.
     *
     * @param email Customer email address to be searched for
     * @param guid Customer guid to be searched for
     * @return List<Customer>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Customer> getAll(
            @QueryParam("email") String email,
            @QueryParam("guid") String guid
    ) {
        if ( Objects.nonNull(guid) ) {
            LOGGER.info("Getting customer by guid '"+guid+"'");
            return customerMySQL.getByGuid(guid).stream().toList();
        }
        if ( Objects.nonNull(email) ) {
            LOGGER.info("Getting customer by email '"+email+"'");
            return customerMySQL.getByEmail(email).stream().toList();
        }
        LOGGER.info("Getting all customers from the database.");
        return customerMySQL.getAll();
    }

    /**
     * Stores the newly posted customer JSON object in the database.
     * The customer email address is a required field and must be
     * a valid email address. If the JSON object is missing the
     * email field or the email is not a valid email address, then
     * an error is returned.
     *
     * @param customer Customer to be inserted
     * @return {@link Response}
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequestBody(
            name = "customer",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Customer.class)
            )
    )
    @Counted(name = INSERT_COUNTER_NAME,
            absolute = true,
            description = INSERT_COUNTER_DESCRIPTION
    )
    @Timed(name = INSERT_TIMER_NAME,
            description = INSERT_TIMER_DESCRIPTION,
            unit = MetricUnits.SECONDS,
            absolute = true
    )
    public Response insertCustomer(Customer customer) {
        try {
            Customer newCustomer = customerMySQL.insert(customer);
            LOGGER.info("Inserted customer with guid '"+newCustomer.getGuid()+"'");
            return Response
                    .status(Response.Status.OK)
                    .entity(newCustomer)
                    .build();
        } catch (CustomerServiceException customerServiceException) {
            insertCustomerErrors.inc();
            LOGGER.info("Error inserting new customer");
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ExceptionMessage(customerServiceException))
                    .build();
        }
    }

    /**
     * Updates the Customer record for the customer associated with
     * the given {guid}. If no record exists for the given {guid},
     * then a 404 not found is returned. If the updated customer information
     * lacks an email address, or has an invalid email address, then
     * a 400 error is returned.
     *
     * The supplied customer information will _overwrite_ what is
     * present in the database. This means if the customer originally
     * had a nameSurname value, and the body of the PUT has no nameSurname
     * supplied, then the field will be set to null in the database.
     *
     * @param guid guid of Customer to be updated
     * @param customer Customer object to replace the customer
     *                 referenced by the {guid}
     * @return {@link Response}
     */
    @Path("/{guid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequestBody(
            name = "customer",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Customer.class)
            )
    )
    @Counted(name = UPDATE_COUNTER_NAME,
            absolute = true,
            description = UPDATE_COUNTER_DESCRIPTION
    )
    @Timed(name = UPDATE_TIMER_NAME,
            description = UPDATE_TIMER_DESCRIPTION,
            unit = MetricUnits.SECONDS,
            absolute = true
    )
    public Response updateCustomer(
            @PathParam("guid") String guid,
            Customer customer
    ) {
        try {
            Customer newCustomer = customerMySQL.update(guid, customer);
            LOGGER.info("Updated customer with guid '"+guid+"'");
            return Response
                    .status(Response.Status.OK)
                    .entity(newCustomer)
                    .build();
        } catch (CustomerNotFoundException customerNotFoundException) {
            LOGGER.info("Could not update customer with guid '"+guid+"', because no customer has that guid");
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ExceptionMessage(customerNotFoundException))
                    .build();
        } catch (CustomerServiceException customerServiceException) {
            LOGGER.info("Error updating customer with guid '"+guid+"'");
            updateCustomerErrors.inc();
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ExceptionMessage(customerServiceException))
                    .build();
        }
    }

    /**
     * Deletes the customer with the given {guid} from the database.
     * If the customer with {guid} is not found in the database, then
     * a 404 response code is returned.
     *
     * @return {@link Response}
     */
    @Path("/{guid}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = DELETE_COUNTER_NAME,
            absolute = true,
            description = DELETE_COUNTER_DESCRIPTION
    )
    @Timed(name = DELETE_TIMER_NAME,
            description = DELETE_TIMER_DESCRIPTION,
            unit = MetricUnits.SECONDS,
            absolute = true
    )
    public Response deleteCustomer(
            @PathParam("guid") String guid
    ) {
        try {
            Customer oldCustomer = customerMySQL.delete(guid);
            LOGGER.info("Deleted customer with guid '"+guid+"'");
            return Response
                    .status(Response.Status.OK)
                    .entity(oldCustomer)
                    .build();
        } catch (CustomerNotFoundException customerNotFoundException) {
            LOGGER.info("Could not delete customer with guid '"+guid+"', because no customer has that guid");
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ExceptionMessage(customerNotFoundException))
                    .build();
        } catch (CustomerServiceException customerServiceException) {
            LOGGER.info("Error deleting customer with guid '"+guid+"'");
            deleteCustomerErrors.inc();
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ExceptionMessage(customerServiceException))
                    .build();
        }
    }
}
