package org.jabst.jabs;

// Database imports
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.hsqldb.HsqlException;

// MessageDigest for SHA256 hash
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// I/O Imports
import java.io.PrintStream;
import java.io.InputStream;
import java.util.Scanner;

// For returning result sets as native objects
import java.util.ArrayList;

public class DatabaseManager {
    private static final String dbfilePrefix = "jdbc:hsqldb:file:";
    private static final String[] SQL_TABLES_GENERAL = {
        "CREATE TABLE CREDENTIALS ("
            +"USERNAME VARCHAR(20),"
            +"PASSWORD VARBINARY(32),"
            +"PRIMARY KEY(USERNAME))",
        "CREATE TABLE CUSTOMERS ("
            +"USERNAME VARCHAR(20),"
            +"NAME VARCHAR(40),"
            +"ADDRESS VARCHAR(255),"
            +"PHONE VARCHAR(10),"
            +"PRIMARY KEY(USERNAME),"
            +"FOREIGN KEY (USERNAME) REFERENCES CREDENTIALS(USERNAME));",
        "CREATE TABLE BUSINESS ("
            +"USERNAME VARCHAR (40),"
            +"BUSINESS_NAME VARCHAR(40),"
            +"OWNER_NAME VARCHAR(40),"
            +"ADDRESS VARCHAR(255),"
            +"PHONE VARCHAR(10),"
            +"PRIMARY KEY (USERNAME))"
    };
    private static final String[] SQL_TABLES_BUSINESS = {
        "CREATE TABLE EMPLOYEE ("
            +    "EMPL_ID INTEGER,"
            +    "EMPL_NAME VARCHAR(40),"
            +    "ADDRESS VARCHAR(255),"
            +    "PHONE VARCHAR(10),"
            +    "PRIMARY KEY(EMPL_ID)"
            +" )",
            
        "CREATE TABLE APPOINTMENTTYPE ("
            +    "TYPE_ID INTEGER,"
            +    "NAME VARCHAR(40),"
            +    "COST_CENTS INTEGER,"
            +    "PRIMARY KEY (TYPE_ID)"
            +")",
            
        "CREATE TABLE APPOINTMENT ("
            +    "APT_ID INTEGER,"
            +    "DATE_AND_TIME DATETIME,"
            +    "APPOINTMENT_TYPE INTEGER,"
            +    "EMPLOYEE INTEGER,"
            +    "CUSTOMER VARCHAR(20)"
            +    "PRIMARY KEY (APT_ID),"
            +    "FOREIGN KEY (APPOINTMENT_TYPE)"
            +       "REFERENCES APPOINTMENTTYPE (TYPE_ID),"
            +    "FOREIGN KEY (EMPLOYEE) REFERENCES EMPLOYEE(EMPL_ID)"
            +    "FOREIGN KEY (CUSTOMER) REFERENCES CUSTOMERS(USERNAME)"
            +")"
    };
    public static final String dbDefaultFileName = "db/credentials_db";
    private Connection generalConnection;
    private Connection businessConnection;
    
    /** Creates a new DatabaseManager
     * Always open the DatabaseManager at program start (call the constructor),
     * and close it at program finish ( see: close() )
     * @param The name of the file to open
     * @throws HsqlException, SQLException
     */
    public DatabaseManager(String dbfile) throws HsqlException, SQLException {
         this.generalConnection = openCreateDatabase(dbfile, SQL_TABLES_GENERAL);
         if (generalConnection == null) {
             throw new SQLException();
         }
    }
    
    /** Creates the database tables given
     *  in case the database is being created for the first time
     *  @return whether the tables could be successfully created
     */
    private boolean createTables(Connection connection, String[] tables) {
        boolean success = false;
        for (String currTable : tables) {
            Statement statement = null;
            try {
                statement = connection.createStatement();
            } catch (SQLException se) {
                System.err.println("Error creating statement for table");
                return false;
            }
            
            try {
                // Statement.execute returns false if no results were returned,
                // including for CREATE statements
                statement.execute(currTable);
                System.out.println("Successfully created table");
                success = true;
            } catch (SQLException se) {
                System.out.println("Failed to create table");
                return false;
            }
        }
        return success;
    }
    
    /** Closes the database connection associated with the manager
        You MUST do this, or data will not be saved on program exit
     */
    public void close() {
        try {
            generalConnection.commit();
            generalConnection.close();
        } catch (SQLException e) {
            // Nah don't bother handling it
            System.err.println(
                "DatabaseManager: Error closing database properly. Continuing."
            );
        }
    }
    
    /** Tries to connect to the given database, and create it if it doesn't exist already
        @param dbFileName The name of the database file to connect to
        @param tables A string array of SQL statements to execute to make the tables in the
        new database
        @return A connection to the database if successful, otherwise null.
     */
    private Connection openCreateDatabase(String dbFileName, String[] tables) {
        Connection c = null;
         try {
             c = DriverManager.getConnection(dbfilePrefix+dbFileName+";ifexists=true", "sa", "");
         } catch (HsqlException hse) {
             System.err.println("HqlException conecting to database'"+dbFileName+"': Doesn't exist");
         }

         catch (SQLException se) {
            try {
                c = DriverManager.getConnection(dbfilePrefix+dbFileName, "sa", "");
            } catch (SQLException sqle) {
                System.err.println(
                    "DriverManager: Error: Cannot connect to general database file (SQL error) (when trying to open new)"
                );
            }
            if (!createTables(c, tables)) {
                System.err.println(
                    "DriverManager: Error: Cannot create tables in database'"+dbFileName+"'"
                 );
            }
         }
        return c;
    }
    
    /** Opens a connection to the business specified with the username
      * The database file is located in db/$username
      * @param String busUsername : The username of the business
      * @return Whether a connection was sucessfully made
      */
    public boolean connectToBusiness(String busUsername) throws SQLException {
        // Look up the business name in the table of businesses
        Statement stmt = generalConnection.createStatement();
        //stmt.execute();
        
        ResultSet rs = stmt.executeQuery("SELECT COUNT(USERNAME) FROM BUSINESS WHERE USERNAME="+busUsername);
        rs.next();
        switch(rs.getInt(1)){
            case 0:
                return false;
            case 1:
                break;
            default:
                throw new AssertionError("Found 2 of business "+busUsername);
                // Never happens
        }
        
        // We now know it exists for certain, but not whether it has a database
        // Open or create the business' database
        this.businessConnection = openCreateDatabase(busUsername, SQL_TABLES_BUSINESS);
        if (this.businessConnection == null) {
            return false;
        }
        return true;
    }
    
    public static byte[] sha256(String message) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No such algorith as SHA-256?");
            e.printStackTrace(System.err);
            return null;
        }
        return md.digest(message.getBytes());
    }
    
    public static void printDigest(byte[] digest, PrintStream ps) {
        for (int i = 0; i < digest.length; ++i) {
            ps.format("%x", digest[i]);
        } ps.println();
    }
    
    /**
     * Not currently used
     * @param digest the digest to convert to a hexadecimal string
     * @return A string representing the byte[] in hexadecimal text
     */
    public static String digestToHexString(byte[] digest) {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < digest.length; ++i) {
            s.append(String.format("%x", digest[i]));
        }
        return s.toString();
    }
    
    /** Asks the database to check if there is a user with the given
      * username and password
      */
    public boolean checkUser (String username, String password)
        throws SQLException {
        byte[] password_hash = sha256(password);
        boolean success = false;

        PreparedStatement statement = generalConnection.prepareStatement(
            "SELECT * from CREDENTIALS WHERE USERNAME='"+username+"'"
        );

        ResultSet rs = statement.executeQuery(); 
        while (rs.next()) {
            String result_username = rs.getString("username");
            byte[] result_password = rs.getBytes("password");
            System.out.format("Input: username,password = %s,%s\n",
                username, digestToHexString(password_hash)
            );
            System.out.format("Result:username,password = %s,%s\n",
                result_username, digestToHexString(result_password)
            );
            
            for (int i = 0; i < result_password.length; ++i) {
                if (result_password[i] != password_hash[i]) {
                    success = false;
                    break;
                }
                success = true;
            }
        }

        rs.close();
        statement.close();

        return success;
    }
    
    /** Adds a user with the username and password to the database
     *  @param username The username of the user
     *  @param password The password of the user, to be hashed with sha256
     *  before being stored
     *  @return Nothing, check for a SQLException. If it was a
     * SQLIntegrityConstraintViolationException, give a message about the
     * username already existing
     */
    private void addUser(String username, String password)
        throws SQLException {

        byte[] password_hash = sha256(password);
        PreparedStatement statement = null;

        statement = generalConnection.prepareStatement(
            "INSERT INTO CREDENTIALS VALUES (?, ?)"
        );

        statement.setString(1, username);
        statement.setBytes(2, password_hash);

        System.out.println("About to execute adding user...");
        statement.execute();

        statement.close();
        // After adding a user, they need to be able to log in again
        generalConnection.commit();
    }

    /** Adds a user with the given arguments
        @param username Must be no longer than 40 characters
        @param password No size limit
        @param name Must be no longer than 40 character
        @param address Must be no longer than 255 characters
        @param phone Must be no longer than 10 characters (no international numbers)
        @throws SQLException if the database size constraints were exceeded
      */
    public void addUser(String username, String password,
        String name, String address, String phone) throws SQLException
    {
        // Add to credentials table
        addUser(username, password);
        
        // Now add to customers table
        PreparedStatement statement = generalConnection.prepareStatement(
            // USERNAME, NAME, ADDRESS, PHONE
            "INSERT INTO CUSTOMERS VALUES (?, ?, ?, ?)"
        );
        statement.setString(1, username);
        statement.setString(2, name);
        statement.setString(3, address);
        statement.setString(4, phone);
        
        statement.execute();
        statement.close();
    }
    
    private void scannerAddUser(Scanner sc) {
        String username, password, name, address, phone;
        byte[] digest;
        
        System.out.print("Enter username: ");
        username = sc.next();
        System.out.println();
        
        System.out.print("Enter password: ");
        password = sc.next();
        System.out.println();
        
        System.out.print("Next up: name, address, phone");
        name = sc.next(); System.out.println();
        address = sc.next(); System.out.println();
        phone = sc.next(); System.out.println();
        
        boolean success = false;
        try {
            addUser(username, password, name, address, phone);
            success = true;
        } catch (SQLException se) {
                
            if (se instanceof SQLIntegrityConstraintViolationException) {
                System.err.println(
                    "Adding user failed: Already a user with that username"
                );
            }
            
            else {
                System.err.println("addUser failed...");
                se.printStackTrace(System.err);
            }
        }
        
        System.out.println(
            success ? "Added user successfully" : "Didn't add user"
        );
    }
    
    /** Checks if there is a business with the given username
        @param username The username to check
        @return Whether the username represents a business or not
        @throws SQLException If the database encountered an error
      */
    public boolean isBusiness(String username) throws SQLException {
        // NYI: Check if in Business(name)
        Statement stmt = generalConnection.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT COUNT(USERNAME) FROM BUSINESS WHERE USERNAME="+username
        );
        
        rs.next();
        switch(rs.getInt(1)) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new AssertionError(
                    "Found more than one business with username="+username
                    );
        }
    }
    
    
    /** Gets all of the appointments in the system within the date range of
     *  7 days starting from today
     *  @return An ArrayList of Appointment objects representing all the 
     *  appointments within the date range.
     */
    public ArrayList<Appointment> getThisWeeksAppointments() throws SQLException {
        ArrayList<Appointment> appointments = new ArrayList<Appointment>();
        Statement stmt = businessConnection.createStatement();
        ResultSet rs = stmt.executeQuery(            "SELECT * FROM Appointment"
            +"WHERE ("
            +"    date_and_time >= DATE_SUB(CURDATE(),  DAYOFWEEK(CURDATE())-1)"
            +"    AND"
            +"    date_and_time <= DATE_SUB(CURDATE(),  DAYOFWEEK(CURDATE())-1) + INTERVAL '7' DAY"
            +")"
            +"ORDER BY DATE_AND_TIME"
        );
        while (rs.next()) {
            try {
                appointments.add(
                    new Appointment(
                        rs.getDate("DATE_AND_TIME"),
                        rs.getInt("APPOINTMENT_TYPE"),
                        rs.getInt("EMPLOYEE"),
                        rs.getString("CUSTOMER")
                    )
                );
            }
            catch (SQLException sqle) {
                System.err.println(
                    "Error getting appointment. Error code: "+sqle.getErrorCode()
                ); 
            }
        }
        return appointments;
    }
    
    /*         "CREATE TABLE APPOINTMENT ("
            +    "APT_ID INTEGER,"
            +    "DATE_AND_TIME DATETIME,"
            +    "APPOINTMENT_TYPE INTEGER,"
            +    "EMPLOYEE INTEGER,"
            +    "CUSTOMER VARCHAR(20)"
            +    "PRIMARY KEY (APT_ID),"
            +    "FOREIGN KEY (APPOINTMENT_TYPE)"
            +       "REFERENCES APPOINTMENTTYPE (TYPE_ID),"
            +    "FOREIGN KEY (EMPLOYEE) REFERENCES EMPLOYEE(EMPL_ID)"
            +    "FOREIGN KEY (CUSTOMER) REFERENCES CUSTOMERS(USERNAME)"
            +")"*/
    
    private void scannerCheckUser(Scanner sc) {
        String username, password;
        byte[] digest;
        
        System.out.print("Enter username: ");
        username = sc.next();
        System.out.println();
        
        System.out.print("Enter password: ");
        password = sc.next();
        System.out.println();
        
        boolean success = false;
        try {
            success = checkUser(username, password);
        } catch (SQLException se) {
                System.err.println("checkUser failed...");
                se.printStackTrace(System.err);
        }
        
        System.out.println(
            success ?
                "Found a user with that username (NYI: and password" :
                "Didn't find a user with that username"
        );
    }
    /**
      * This main exists to interactively test the code in DatabaseManager. It is
      * not the entry point to the actual application
      */
    public static void main (String[] args) throws SQLException, HsqlException{
        DatabaseManager dbm = new DatabaseManager(dbDefaultFileName);
        System.out.println("Connecting to database: "+dbDefaultFileName);
        Scanner sc = new Scanner(System.in);
        //sc.useDelimiter("\n");
        String input;
        while(true) {
            System.out.print("> ");
            input = sc.next();
            sc.nextLine();
            switch (input) {
                case "add":
                    dbm.scannerAddUser(sc);
                    break;
                case "check":
                    dbm.scannerCheckUser(sc);
                    break;
                case "quit":
                    dbm.close();
                    return;
                default:
                    System.out.println("Not a valid command. Received:"+input);
            }
        }
    }
}
