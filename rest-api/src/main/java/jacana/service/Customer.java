package jacana.service;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

/**
 * Represents the Customer object. Stores information
 * that is specific to a customer, e.g. name, phone,
 * email and a unique identifier.
 *
 * The object has a routine that will validate the
 * Customer information. In this case a valid customer
 * must have an email address and it must be a valid
 * email address.
 *
 * Customer objects can be compared, but equality is limited
 * to all fields with the exception of the unique identifier.
 * Since customer objects can be created without a unique
 * identifier and then compared to a customer object with a
 * unique identifier the test of equality will ignore the
 * unique identifier if one or both of the customer objects
 * has a null unique identifier. BUT, if both customer objects
 * have a unique identifier, and they are not equal, then
 * the customer objects are not considered equal.
 */
@JsonInclude(ALWAYS)
public class Customer {

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    private String guid; /* globally unique identifier of object */
    private String namePrefix;
    private String nameSuffix;
    private String nameSurname; /* first name */
    private String nameMiddle;
    private String nameFamily; /* last name */
    private String email;
    private String phoneNumber;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String getNameSuffix() {
        return nameSuffix;
    }

    public void setNameSuffix(String nameSuffix) {
        this.nameSuffix = nameSuffix;
    }

    public String getNameSurname() {
        return nameSurname;
    }

    public void setNameSurname(String nameSurname) {
        this.nameSurname = nameSurname;
    }

    public String getNameMiddle() {
        return nameMiddle;
    }

    public void setNameMiddle(String nameMiddle) {
        this.nameMiddle = nameMiddle;
    }

    public String getNameFamily() {
        return nameFamily;
    }

    public void setNameFamily(String nameFamily) {
        this.nameFamily = nameFamily;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns true if the Customer object has an email and that
     * email is a valid email address. Otherwise an Exception is
     * thrown and the message in the Exception explains the
     * reason for being invalid.
     */
    public void validate() throws Exception {
        if (Objects.isNull(getEmail())) {
            throw new Exception("Customer email address must not be null.");
        }
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(getEmail());
        if (! matcher.matches() ) {
            throw new Exception("Customer email address is not a valid email address.");
        }
    }

    public String toString() {
        return "Customer.guid='" + guid + "'";
    }

    /**
     * Returns true if all fields in the Customer objects are identical
     * with the exception of the guid field.
     *
     * The check for equality of the guid field is when both objects have
     * a non-null guid and they are equal. True is still returned if one
     * or both of the objects has a null guid field.
     *
     * All other fields are considered equal if they are identical by
     * definition of the String.equals() method. If both fields are null,
     * then they are considered equal as well, but if one is null and
     * the other is not, then they are not equal which is the opposite
     * of how the guid field is handled for equality.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer customer = (Customer) o;
        if (Objects.nonNull(getGuid()) && Objects.nonNull(customer.getGuid()) && !getGuid().equals(customer.getGuid())) {
            /*
             * guid comparisons are a bit more permissive when considering nulls.
             * Only consider it a mismatch if both guids are not null and not equal.
             */
            return false;
        }
        return nullCompare(getEmail(), customer.getEmail()) &&
                nullCompare(getNamePrefix(), customer.getNamePrefix()) &&
                nullCompare(getNameSurname(), customer.getNameSurname()) &&
                nullCompare(getNameMiddle(), customer.getNameMiddle()) &&
                nullCompare(getNameFamily(), customer.getNameFamily()) &&
                nullCompare(getPhoneNumber(), customer.getPhoneNumber()) &&
                nullCompare(getNameSuffix(), customer.getNameSuffix());
    }

    /**
     * Returns true if both s1 and s2 are null, or have equal non-null
     * values. Returns false if their non-null values do not match
     * or one is null and the other is not.
     *
     * @param s1
     * @param s2
     * @return
     */
    private boolean nullCompare(String s1, String s2) {
        if ( Objects.nonNull(s1) && Objects.nonNull(s2) ) {
            return s1.equals(s2);
        } else {
            return Objects.isNull(s1) && Objects.isNull(s2);
        }
    }
}
