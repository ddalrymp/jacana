package jacana.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

@ApplicationScoped
public class CustomerServiceMySQL implements CustomerService<Customer> {

    private static final Logger LOGGER = Logger.getLogger(CustomerServiceMySQL.class.getName());

    private boolean initializedDatabase = false;
    private String url;
    private String user;
    private String password;
    private String createCustomersTableSql;
    private String createCustomersGuidIndexSql;
    private String createCustomersEmailIndexSql;
    private String selectAllCustomersSql;
    private String selectCustomerByGuidSql;
    private String selectCustomerByEmailSql;
    private String insertCustomerSql;
    private String updateCustomerSql;
    private String deleteCustomerSql;

    public CustomerServiceMySQL() throws Exception {
        initializeDatabase();
    }

    @Inject
    public CustomerServiceMySQL(@ConfigProperty(name = "database.user") String user,
                                @ConfigProperty(name = "database.password") String password,
                                @ConfigProperty(name = "database.host") String host,
                                @ConfigProperty(name = "database.port") String port,
                                @ConfigProperty(name = "database.schema") String schema,
                                @ConfigProperty(name = "database.create_customers_table") String createCustomersTableSql,
                                @ConfigProperty(name = "database.create_customers_guid_index") String createCustomersGuidIndexSql,
                                @ConfigProperty(name = "database.create_customers_email_index") String createCustomersEmailIndexSql,
                                @ConfigProperty(name = "database.select_all_customers") String selectAllCustomersSql,
                                @ConfigProperty(name = "database.select_customer_by_guid") String selectCustomerByGuidSql,
                                @ConfigProperty(name = "database.select_customer_by_email") String selectCustomerByEmailSql,
                                @ConfigProperty(name = "database.insert_customer") String insertCustomerSql,
                                @ConfigProperty(name = "database.update_customer") String updateCustomerSql,
                                @ConfigProperty(name = "database.delete_customer") String deleteCustomerSql) throws Exception {
        this.url = "jdbc:mysql://"+host+":"+port+"/"+schema;
        this.user = user;
        this.password = password;
        this.createCustomersTableSql = createCustomersTableSql;
        this.createCustomersGuidIndexSql = createCustomersGuidIndexSql;
        this.createCustomersEmailIndexSql = createCustomersEmailIndexSql;
        this.selectAllCustomersSql = selectAllCustomersSql;
        this.selectCustomerByGuidSql = selectCustomerByGuidSql;
        this.selectCustomerByEmailSql = selectCustomerByEmailSql;
        this.insertCustomerSql = insertCustomerSql;
        this.updateCustomerSql = updateCustomerSql;
        this.deleteCustomerSql = deleteCustomerSql;
    }

    private Customer fromResultSet(ResultSet resultSet) throws Exception {
        Customer customer = new Customer();
        customer.setGuid(resultSet.getString("guid"));
        customer.setEmail(resultSet.getString("email"));
        return customer;
    }

    private List<Customer> getCustomers(String guid, String email) {
        List<Customer> listOfCustomers = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            initializeDatabase();
            connection = getConnection();
            if ( Objects.nonNull(guid) && Objects.isNull(email) ) {
                preparedStatement = connection.prepareStatement(this.selectCustomerByGuidSql);
                preparedStatement.setString(1, guid);
            }
            if ( Objects.isNull(guid) && Objects.nonNull(email) ) {
                preparedStatement = connection.prepareStatement(this.selectCustomerByEmailSql);
                preparedStatement.setString(1, email);
            }
            if ( Objects.isNull(preparedStatement) ) {
                preparedStatement = connection.prepareStatement(this.selectAllCustomersSql);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                listOfCustomers.add(fromResultSet(resultSet));
            }
        } catch (Exception ex) {
            LOGGER.info("Exception: " + ex.getMessage());
        } finally {
            try {
                if (Objects.nonNull(preparedStatement) ) {
                    preparedStatement.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception: " + ex.getMessage());
            }
            try {
                if (Objects.nonNull(connection) ) {
                    connection.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception: " + ex.getMessage());
            }
        }
        return listOfCustomers;
    }

    @Override
    public List<Customer> getAll() {
        return getCustomers(null, null);
    }

    @Override
    public Optional<Customer> getByGuid(String guid) {
        List<Customer> customers = getCustomers(guid, null);
        if ( customers.isEmpty() ) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(customers.get(0));
        }
    }

    @Override
    public Optional<Customer> getByEmail(String email) {
        List<Customer> customers = getCustomers(null, email);
        if ( customers.isEmpty() ) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(customers.get(0));
        }
    }

    @Override
    public Customer insert(Customer customer) throws CustomerServiceException {
        /*
         * Simple validations.
         */
        if ( Objects.isNull(customer) ) {
            throw new CustomerServiceException("Customer object may not be null.");
        }
        if ( Objects.isNull(customer.getEmail()) ) {
            throw new CustomerServiceException("Customer email may not be null.");
        }
        if ( Objects.isNull(customer.getGuid()) ) {
            customer.setGuid(UUID.randomUUID().toString());
        }
        /*
         * Execute the insert operation.
         */
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(this.insertCustomerSql);
            preparedStatement.setString(1, customer.getGuid());
            preparedStatement.setString(2, customer.getEmail());
            preparedStatement.execute();
            connection.close();
            LOGGER.info("Inserted Customer with guid='"+customer.getGuid()+"'");
        } catch (Exception ex) {
            LOGGER.info("Exception inserting Customer: " + ex.getMessage());
            throw new CustomerServiceException(ex.getMessage());
        } finally {
            try {
                if (Objects.nonNull(preparedStatement) ) {
                    preparedStatement.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception closing prepared statement: " + ex.getMessage());
            }
            try {
                if (Objects.nonNull(connection) ) {
                    connection.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception closing database connection: " + ex.getMessage());
            }
        }
        /*
         * Return the inserted customer.
         */
        return customer;
    }

    @Override
    public Customer update(String guid, Customer customer) throws CustomerServiceException {
        /*
         * Simple validations.
         */
        if ( Objects.isNull(guid) ) {
            throw new CustomerServiceException("guid of Customer to update may not be null.");
        }
        if ( Objects.isNull(customer) ) {
            throw new CustomerServiceException("New Customer may not be null.");
        }
        if ( Objects.isNull(customer.getEmail()) ) {
            throw new CustomerServiceException("Customer email may not be null.");
        }
        Optional<Customer> oldCustomer = getByGuid(guid);
        if ( oldCustomer.isEmpty() ) {
            throw new CustomerServiceException("Customer with guid '"+guid+"' cannot be found and therefore cannot be updated.");
        }
        /*
         * Execute the update operation.
         */
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(this.updateCustomerSql);
            preparedStatement.setString(1, customer.getEmail());
            preparedStatement.setString(2, guid);
            preparedStatement.execute();
            connection.close();
            LOGGER.info("Updated Customer with guid='"+guid+"'");
        } catch (Exception ex) {
            LOGGER.info("Exception updating Customer with guid='"+guid+"': " + ex.getMessage());
            throw new CustomerServiceException(ex.getMessage());
        } finally {
            try {
                if (Objects.nonNull(preparedStatement) ) {
                    preparedStatement.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception closing prepared statement: " + ex.getMessage());
            }
            try {
                if (Objects.nonNull(connection) ) {
                    connection.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception closing database connection: " + ex.getMessage());
            }
        }
        /*
         * Return the updated customer.
         */
        return getByGuid(guid).get();
    }

    @Override
    public Customer delete(String guid) throws CustomerServiceException {
        /*
         * Simple validations.
         */
        if ( Objects.isNull(guid) ) {
            throw new CustomerServiceException("guid of Customer to delete may not be null.");
        }
        Optional<Customer> oldCustomer = getByGuid(guid);
        if ( oldCustomer.isEmpty() ) {
            throw new CustomerServiceException("Customer with guid '"+guid+"' cannot be found and therefore cannot be deleted.");
        }
        /*
         * Execute the delete operation.
         */
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(this.deleteCustomerSql);
            preparedStatement.setString(1, guid);
            preparedStatement.execute();
            connection.close();
            LOGGER.info("Deleted Customer with guid='"+guid+"'");
        } catch (Exception ex) {
            LOGGER.info("Exception deleting Customer with guid='"+guid+"': " + ex.getMessage());
            throw new CustomerServiceException(ex.getMessage());
        } finally {
            try {
                if (Objects.nonNull(preparedStatement) ) {
                    preparedStatement.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception closing prepared statement: " + ex.getMessage());
            }
            try {
                if (Objects.nonNull(connection) ) {
                    connection.close();
                }
            } catch (Exception ex) {
                LOGGER.info("Exception closing database connection: " + ex.getMessage());
            }
        }
        /*
         * Return deleted customer.
         */
        return oldCustomer.get();
    }

    public Connection getConnection() throws Exception {
        // below two lines are used for connectivity.
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Lazy database initialization. Only executes once and permits
     * failures if the tables and indices already exist. This method
     * mainly exists to ensure the schema is properly set up to
     * store Customer objects and respect the rules of Customer objects.
     *
     * @throws Exception
     */
    public void initializeDatabase() throws Exception {
        if (!initializedDatabase) {
            try {
                Connection connection = getConnection();
                Statement statement = connection.createStatement();
                try {
                    statement.execute(this.createCustomersTableSql);
                    LOGGER.info("Created Customers table.");
                } catch (Exception ex) {
                    LOGGER.info("Exception creating Customers table: " + ex.getMessage());
                }
                try {
                    statement.execute(this.createCustomersGuidIndexSql);
                    LOGGER.info("Created Customers.guid index.");
                } catch (Exception ex) {
                    LOGGER.info("Exception creating Customers.guid index: " + ex.getMessage());
                }
                try {
                    statement.execute(this.createCustomersEmailIndexSql);
                    LOGGER.info("Created Customers.email index.");
                } catch (Exception ex) {
                    LOGGER.info("Exception creating Customers.email index: " + ex.getMessage());
                }
            } catch (Exception ex) {
                LOGGER.info("Exception initializing database: " + ex.getMessage());
            }
        }
        this.initializedDatabase = true;
    }
}
