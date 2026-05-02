package com.routex.dal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the single shared JDBC connection to SQL Server.
 *
 * Design Pattern: SINGLETON (GoF Creational)
 *   - Only one DatabaseConnection instance exists throughout the application lifetime.
 *   - Thread-safe via double-checked locking.
 *
 * GRASP: Controller — centralises all low-level connection concerns so that
 *        DAOs do not need to know how to connect to the database.
 *
 * HOW TO CONFIGURE:
 *   Update DB_URL, DB_USER, and DB_PASSWORD below to match your SQL Server instance.
 *   The JDBC URL format for SQL Server is:
 *     jdbc:sqlserver://<host>:<port>;databaseName=<db>;encrypt=false;trustServerCertificate=true
 */
public class DatabaseConnection {

    // =========================================================================
    // *** UPDATE THESE VALUES BEFORE RUNNING THE APPLICATION ***
    // =========================================================================
    private static final String DB_URL      = getEnvOrDefault("ROUTEX_DB_URL",
                                            "jdbc:sqlserver://localhost:1433;"
                                            + "databaseName=RouteX_DB;"
                                            + "integratedSecurity=true;"
                                            + "encrypt=false;"
                                            + "trustServerCertificate=true;"
                                            + "authentication=NotSpecified");
    private static final String DB_USER     = getEnvOrDefault("ROUTEX_DB_USER", "");
    private static final String DB_PASSWORD = getEnvOrDefault("ROUTEX_DB_PASSWORD", "");
    // =========================================================================

    /** The single instance (volatile for thread-safe double-checked locking). */
    private static volatile DatabaseConnection instance;

    /** The underlying JDBC connection. */
    private Connection connection;

    /** Private constructor prevents external instantiation. */
    private DatabaseConnection() {
        try {
            // Load the SQL Server driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
            // Use Windows Authentication if credentials are empty, else use SQL Server Authentication
            if (DB_USER == null || DB_USER.isEmpty()) {
                this.connection = DriverManager.getConnection(DB_URL);
            } else {
                this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
            System.out.println("[DB] Connection established to RouteX_DB");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("[DB] SQL Server JDBC driver not found on classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("[DB] Failed to connect: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the sole DatabaseConnection instance (double-checked locking).
     *
     * @return the singleton DatabaseConnection
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the active JDBC Connection, re-connecting automatically if
     * the connection was dropped (e.g., DB restart during a long session).
     *
     * @return a live JDBC Connection
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DB] Re-establishing lost connection...");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
        } catch (SQLException e) {
            throw new RuntimeException("[DB] Could not re-connect: " + e.getMessage(), e);
        }
        return connection;
    }

    /** Gracefully closes the connection (call on application shutdown). */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}