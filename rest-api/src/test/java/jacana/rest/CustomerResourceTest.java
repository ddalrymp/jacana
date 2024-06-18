package jacana.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import jacana.service.Customer;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@HelidonTest
public class CustomerResourceTest {

    @Inject
    private MetricRegistry registry;

    @Inject
    private WebTarget target;

    @ClassRule
    public static MySQLContainer mySQLContainer
            = new MySQLContainer("mysql:8.0")
            .withDatabaseName("jacana")
            .withUsername("user")
            .withPassword("password");

    @BeforeAll
    static void startDb() throws SQLException {
        List<String> portBindings = new ArrayList<>();
        portBindings.add("3306:3306"); // hostPort:containerPort
        mySQLContainer.setPortBindings(portBindings);
        mySQLContainer.start();
    }

    @AfterAll
    static void stopDb(){
        mySQLContainer.stop();
    }

    @Test
    void testHealth() {
        Response response = target
                .path("health")
                .request()
                .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    void testInsertCustomer() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer.setEmail("email-"+randomString+"@example.com");
        testCustomer.setNamePrefix("Prefix"+randomString);
        testCustomer.setNameSurname("Surname"+randomString);
        testCustomer.setNameMiddle("Middle"+randomString);
        testCustomer.setNameFamily("Family"+randomString);
        testCustomer.setNameSuffix("Suffix"+randomString);
        testCustomer.setPhoneNumber("Phone"+randomString);
        String requestBody = mapper.writeValueAsString(testCustomer);
        String createdCustomerGuid = null;

        Counter counter = registry.counter("insertCustomer");
        double before = counter.getCount();
        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            assertThat(
                    "Response to creating a customer should equal the customer sent to be created.",
                    createdCustomer.equals(testCustomer), is(true)
            );
            createdCustomerGuid = createdCustomer.getGuid();
            double after = counter.getCount();
            assertThat(
                    "Metric count of insertCustomer should increase by one.",
                    after - before, is(1d)
            );
        }

        List<Customer> customers = target
                .path("customers")
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        boolean foundCustomer = false;
        for ( Customer customer : customers ) {
            if ( customer.equals(testCustomer) ) {
                foundCustomer = true;
                break;
            }
        }
        assertThat(
                "Test customer should be found when listing all customers.",
                foundCustomer,is(true)
        );

        List<Customer> customersByEmail = target
                .path("customers")
                .queryParam("email",testCustomer.getEmail())
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        assertThat(
                "Getting a customer by email that was just inserted should return a size of 1.",
                customersByEmail.size(), is(1)
        );
        assertThat(
                "The one customer returned when getting customers by email should match the test customer that was inserted.",
                customersByEmail.get(0).equals(testCustomer),is(true)
        );

        List<Customer> customersByGuid = target
                .path("customers")
                .queryParam("guid", createdCustomerGuid)
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        assertThat(
                "Getting a customer by guid that was just inserted should return a size of 1.",
                customersByGuid.size(), is(1)
        );
        assertThat(
                "The one customer returned when getting customers by guid should match the test customer that was inserted.",
                customersByGuid.get(0).equals(testCustomer),is(true)
        );
    }

    @Test
    void testInsertCustomerInvalidEmail() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer = new Customer();
        testCustomer.setEmail("email-" + UUID.randomUUID().toString() + "");
        String requestBody = mapper.writeValueAsString(testCustomer);

        Counter counter = registry.counter("insertCustomerErrors");
        double before = counter.getCount();
        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 400 when creating a customer with an invalid email address.",
                    r.getStatus(), is(400)
            );
            double after = counter.getCount();
            assertThat(
                    "Metric count of insertCustomerErrors should increase by one.",
                    after - before, is(1d)
            );
        }
    }

    @Test
    void testInsertCustomerMissingEmail() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer = new Customer();
        String requestBody = mapper.writeValueAsString(testCustomer);

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 400 when creating a customer with a missing email address.",
                    r.getStatus(), is(400)
            );
        }
    }

    @Test
    void testInsertCustomerDuplicateEmail() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer1 = new Customer();
        Customer testCustomer2 = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer1.setEmail("email1-"+randomString+"@example.com");
        testCustomer2.setEmail(testCustomer1.getEmail());

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(mapper.writeValueAsString(testCustomer1), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
        }

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(mapper.writeValueAsString(testCustomer2), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 400 error when trying to insert a customer with an email matching an existing customer.",
                    r.getStatus(), is(400)
            );
        }

    }

    @Test
    void testUpdateCustomer() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer.setEmail("email-"+randomString+"@example.com");
        testCustomer.setNamePrefix("Prefix"+randomString);
        testCustomer.setNameSurname("Surname"+randomString);
        testCustomer.setNameMiddle("Middle"+randomString);
        testCustomer.setNameFamily("Family"+randomString);
        testCustomer.setNameSuffix("Suffix"+randomString);
        testCustomer.setPhoneNumber("Phone"+randomString);
        String requestBody = mapper.writeValueAsString(testCustomer);
        String createdCustomerGuid = null;
        String updatedCustomerGuid = null;

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            assertThat(
                    "Response to creating a customer should equal the customer sent to be created.",
                    createdCustomer.equals(testCustomer), is(true)
            );
            createdCustomerGuid = createdCustomer.getGuid();
        }

        Customer updateCustomer = new Customer();
        randomString = UUID.randomUUID().toString();
        updateCustomer.setEmail("email-"+randomString+"@example.com");
        updateCustomer.setNamePrefix("Prefix"+randomString);
        updateCustomer.setNameSurname("Surname"+randomString);
        updateCustomer.setNameMiddle("Middle"+randomString);
        updateCustomer.setNameFamily("Family"+randomString);
        updateCustomer.setNameSuffix("Suffix"+randomString);
        updateCustomer.setPhoneNumber("Phone"+randomString);
        requestBody = mapper.writeValueAsString(updateCustomer);

        Counter counter = registry.counter("updateCustomer");
        double before = counter.getCount();
        try (Response r = target
                .path("customers/"+createdCustomerGuid)
                .request()
                .put(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer updatedCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            updatedCustomerGuid = updatedCustomer.getGuid();
            assertThat(
                    "Response to update a customer should equal the customer details sent for update.",
                    updatedCustomer.equals(updateCustomer), is(true)
            );
            double after = counter.getCount();
            assertThat(
                    "Metric count of updateCustomer should increase by one.",
                    after - before, is(1d)
            );
        }

        List<Customer> customers = target
                .path("customers")
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        boolean foundCustomer = false;
        for ( Customer customer : customers ) {
            if ( customer.equals(updateCustomer) ) {
                foundCustomer = true;
                break;
            }
        }
        assertThat(
                "Updated customer should be found when listing all customers because test customer was updated.",
                foundCustomer,is(true)
        );

        List<Customer> customersByEmail = target
                .path("customers")
                .queryParam("email",updateCustomer.getEmail())
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        assertThat(
                "Getting a customer by email that was just updated should return a size of 1.",
                customersByEmail.size(), is(1)
        );

        List<Customer> customersByGuid = target
                .path("customers")
                .queryParam("guid", updatedCustomerGuid)
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        assertThat(
                "Getting a customer by guid that was just updated should return a size of 1.",
                customersByGuid.size(), is(1)
        );
    }

    @Test
    void testUpdateCustomerNotFound() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer.setEmail("email-"+randomString+"@example.com");
        testCustomer.setNamePrefix("Prefix"+randomString);
        testCustomer.setNameSurname("Surname"+randomString);
        testCustomer.setNameMiddle("Middle"+randomString);
        testCustomer.setNameFamily("Family"+randomString);
        testCustomer.setNameSuffix("Suffix"+randomString);
        testCustomer.setPhoneNumber("Phone"+randomString);
        String requestBody = mapper.writeValueAsString(testCustomer);

        Counter counter = registry.counter("updateCustomer");
        double before = counter.getCount();
        try (Response r = target
                .path("customers/" + randomString)
                .request()
                .put(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 404 because the customer to update is not in the database.",
                    r.getStatus(), is(404)
            );
            double after = counter.getCount();
            assertThat(
                    "Metric count of updateCustomer should increase by one.",
                    after - before, is(1d)
            );
        }
    }

    @Test
    void testUpdateCustomerDuplicateEmail() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer1 = new Customer();
        Customer testCustomer2 = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer1.setEmail("email1-"+randomString+"@example.com");
        testCustomer2.setEmail("email2-"+randomString+"@example.com");
        String createdCustomerGuid1 = null;
        String createdCustomerGuid2 = null;

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(mapper.writeValueAsString(testCustomer1), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            createdCustomerGuid1 = createdCustomer.getGuid();
        }

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(mapper.writeValueAsString(testCustomer2), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
        }

        Counter counter = registry.counter("updateCustomerErrors");
        double before = counter.getCount();
        try (Response r = target
                .path("customers/"+createdCustomerGuid1)
                .request()
                .put(Entity.entity(mapper.writeValueAsString(testCustomer2), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 400 error because updating a customer to have an email which already belongs to another customer is not valid.",
                    r.getStatus(), is(400)
            );
            double after = counter.getCount();
            assertThat(
                    "Metric count of updateCustomerErrors should increase by one.",
                    after - before, is(1d)
            );
        }
    }

    @Test
    void testUpdateCustomerMissingEmail() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer1 = new Customer();
        Customer testCustomer2 = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer1.setEmail("email1-"+randomString+"@example.com");
        testCustomer2.setEmail(null);
        String createdCustomerGuid1 = null;

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(mapper.writeValueAsString(testCustomer1), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            createdCustomerGuid1 = createdCustomer.getGuid();
        }

        Counter counter = registry.counter("updateCustomerErrors");
        double before = counter.getCount();
        try (Response r = target
                .path("customers/"+createdCustomerGuid1)
                .request()
                .put(Entity.entity(mapper.writeValueAsString(testCustomer2), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 400 error because updating a customer to have an empty/null email is not valid.",
                    r.getStatus(), is(400)
            );
            double after = counter.getCount();
            assertThat(
                    "Metric count of updateCustomerErrors should increase by one.",
                    after - before, is(1d)
            );
        }
    }

    @Test
    void testUpdateCustomerInvalidEmail() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer1 = new Customer();
        Customer testCustomer2 = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer1.setEmail("email1-"+randomString+"@example.com");
        testCustomer2.setEmail("email1-"+randomString+""); /* invalid email */
        String createdCustomerGuid1 = null;

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(mapper.writeValueAsString(testCustomer1), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            createdCustomerGuid1 = createdCustomer.getGuid();
        }

        Counter counter = registry.counter("updateCustomerErrors");
        double before = counter.getCount();
        try (Response r = target
                .path("customers/"+createdCustomerGuid1)
                .request()
                .put(Entity.entity(mapper.writeValueAsString(testCustomer2), MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 400 error because updating a customer to have an empty/null email is not valid.",
                    r.getStatus(), is(400)
            );
            System.out.println("DEREK : " + r.readEntity(String.class));
            double after = counter.getCount();
            assertThat(
                    "Metric count of updateCustomerErrors should increase by one.",
                    after - before, is(1d)
            );
        }
    }

    @Test
    void testDeleteCustomer() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Customer testCustomer = new Customer();
        String randomString = UUID.randomUUID().toString();
        testCustomer.setEmail("email-"+randomString+"@example.com");
        testCustomer.setNamePrefix("Prefix"+randomString);
        testCustomer.setNameSurname("Surname"+randomString);
        testCustomer.setNameMiddle("Middle"+randomString);
        testCustomer.setNameFamily("Family"+randomString);
        testCustomer.setNameSuffix("Suffix"+randomString);
        testCustomer.setPhoneNumber("Phone"+randomString);
        String requestBody = mapper.writeValueAsString(testCustomer);
        String createdCustomerGuid = null;

        try (Response r = target
                .path("customers")
                .request()
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer createdCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            assertThat(
                    "Response to creating a customer should equal the customer sent to be created.",
                    createdCustomer.equals(testCustomer), is(true)
            );
            createdCustomerGuid = createdCustomer.getGuid();
        }

        Counter counter = registry.counter("deleteCustomer");
        double before = counter.getCount();
        try (Response r = target
                .path("customers/"+createdCustomerGuid)
                .request()
                .delete()) {
            assertThat(
                    "Response code should be 200 OK",
                    r.getStatus(), is(200)
            );
            Customer deletedCustomer = mapper.readValue(r.readEntity(String.class), Customer.class);
            assertThat(
                    "Response to deleting a customer should equal the customer sent to be deleted.",
                    deletedCustomer.equals(testCustomer), is(true)
            );
            double after = counter.getCount();
            assertThat(
                    "Metric count of deleteCustomer should increase by one.",
                    after - before, is(1d)
            );
        }

        List<Customer> customers = target
                .path("customers")
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        boolean foundCustomer = false;
        for ( Customer customer : customers ) {
            if ( customer.equals(testCustomer) ) {
                foundCustomer = true;
                break;
            }
        }
        assertThat(
                "Test customer should NOT be found when listing all customers because test customer was deleted.",
                foundCustomer,is(false)
        );

        List<Customer> customersByEmail = target
                .path("customers")
                .queryParam("email",testCustomer.getEmail())
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        assertThat(
                "Getting a customer by email that was just deleted should return a size of 0.",
                customersByEmail.size(), is(0)
        );

        List<Customer> customersByGuid = target
                .path("customers")
                .queryParam("guid", createdCustomerGuid)
                .request()
                .get(Response.class)
                .readEntity(new GenericType<List<Customer>>() {});
        assertThat(
                "Getting a customer by guid that was just deleted should return a size of 0.",
                customersByGuid.size(), is(0)
        );
    }

    @Test
    void testDeleteCustomerNotFound() throws JsonProcessingException {
        String randomString = UUID.randomUUID().toString();

        Counter counter = registry.counter("deleteCustomer");
        double before = counter.getCount();
        try (Response r = target
                .path("customers/" + randomString)
                .request()
                .delete()) {
            assertThat(
                    "Response code should be 404 because the customer to delete is not in the database.",
                    r.getStatus(), is(404)
            );
            double after = counter.getCount();
            assertThat(
                    "Metric count of deleteCustomer should increase by one.",
                    after - before, is(1d)
            );
        }
    }
}