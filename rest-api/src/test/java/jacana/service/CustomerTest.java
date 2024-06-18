package jacana.service;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@HelidonTest
public class CustomerTest {

    @Test
    void testValidateMissingEmail() {
        Customer customer1 = new Customer();
        Throwable exception = assertThrows(Exception.class, () -> customer1.validate());
    }

    @Test
    void testValidateInvalidEmail() {
        Customer customer1 = new Customer();
        customer1.setEmail("not-a-good-email");
        Throwable exception = assertThrows(Exception.class, () -> customer1.validate());
        assertEquals("Customer email address is not a valid email address.", exception.getMessage());
    }

    @Test
    void testValidateValid() {
        Customer customer1 = new Customer();
        customer1.setEmail("foo@example.com");
        try {
            customer1.validate();
        } catch (Exception ex) {
            fail("Exception should not be thrown when calling validate() on a valid customer.");
        }
    }

    @Test
    void testCompare() {
        Customer customer1 = new Customer();
        Customer customer2 = new Customer();
        customer1.setGuid("abc");
        assertThat(
                "Customers should be equal because one has a null guid and all other fields are null.",
                customer1.equals(customer2), is(true)
        );
        customer2.setGuid("abc");
        assertThat(
                "Customers should be equal because both have identical guid values, and all other fields are null.",
                customer1.equals(customer2), is(true)
        );
        customer1.setGuid(null);
        assertThat(
                "Customers should be equal because one has a null guid and all other fields are null.",
                customer1.equals(customer2), is(true)
        );
        customer1.setGuid("xyz");
        assertThat(
                "Customers should NOT be equal because they have non-null guids that do not match.",
                customer1.equals(customer2), is(false)
        );
    }

    @Test
    void testCompareEmails() {
        Customer customer1 = new Customer();
        Customer customer2 = new Customer();
        customer1.setEmail("foo@example.com");
        customer2.setEmail(null);
        assertThat(
                "Customers should not be equal because one has a null email while the other does not.",
                customer1.equals(customer2), is(false)
        );
        customer1.setEmail(null);
        customer2.setEmail("foo@example.com");
        assertThat(
                "Customers should not be equal because one has a null email while the other does not.",
                customer1.equals(customer2), is(false)
        );
        customer1.setEmail("foo@example.com");
        customer2.setEmail("foo@example.com");
        assertThat(
                "Customers should be equal because they have identical, non-null, email addresses.",
                customer1.equals(customer2), is(true)
        );
        customer1.setEmail(null);
        customer2.setEmail(null);
        assertThat(
                "Customers should be equal because they both have null email addresses.",
                customer1.equals(customer2), is(true)
        );
    }
}
