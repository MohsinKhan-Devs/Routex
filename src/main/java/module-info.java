module com.routex {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    // Ensure the SQL Server driver is in your pom.xml for this to work
    requires com.microsoft.sqlserver.jdbc;

    // Allows JavaFX to load your FXML and Main classes
    opens com.routex to javafx.fxml, javafx.graphics;
    
    // If you create a 'ui' package for controllers, open it here
    // opens com.routex.ui to javafx.fxml;

    // Required for TableView to access your model properties
    opens com.routex.model to javafx.base;

    // Exports for the rest of the system
    exports com.routex;
    exports com.routex.dal;
    exports com.routex.model;
    exports com.routex.service;
    exports com.routex.enums;
    exports com.routex.ui;
}