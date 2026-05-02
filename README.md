# RouteX

RouteX is a JavaFX + SQL Server inventory and fleet management system built with a 3-tier architecture.

## Features

Implemented use cases:
- UC01 - Login / Authenticate
- UC02 - Manage Inventory Stock
- UC04 - Generate Shipment Order
- UC05 - Approve / Reject Shipment Order
- UC06 - Assign Vehicle to Shipment
- UC09 - Update Delivery Status
- UC10 - Report Vehicle Issue
- UC11 - Manage User Accounts

## Requirements

- JDK 17
- Maven
- Microsoft SQL Server
- SQL Server JDBC access enabled

## Database Setup

1. Open SQL Server Management Studio or another SQL client.
2. Run [src/main/resources/db/schema.sql](src/main/resources/db/schema.sql).
3. Run [src/main/resources/db/seed.sql](src/main/resources/db/seed.sql).
4. Confirm the database name is `RouteX_DB`.

## Configuration

The application reads these optional environment variables:
- `ROUTEX_DB_URL`
- `ROUTEX_DB_USER`
- `ROUTEX_DB_PASSWORD`

If they are not set, the app uses the defaults in `DatabaseConnection`.

Default connection values:
- URL: `jdbc:sqlserver://localhost:1433;databaseName=RouteX_DB;encrypt=false;trustServerCertificate=true`
- User: Windows Integrated Authentication (recommended) or `sa`
- Password: `Pass123` (if using SQL auth)

## Seed Login Accounts

Use these accounts after running the seed script:
- System Admin: `s.jenkins@routex.com` / `Admin@123`
- Inventory Manager: `m.thorne@routex.com` / `Manager@123`
- Fleet Dispatcher: `e.rodriguez@routex.com` / `Dispatcher@123`
- Field Driver: `j.howlett@routex.com` / `Driver1@123`

## Run

From the project root:

```bash
mvn clean package
mvn javafx:run
```

If `mvn` is not recognized on Windows, Maven is not installed or not added to PATH. Install Apache Maven, then add its `bin` folder to your PATH and reopen the terminal. You can also run the project from IntelliJ IDEA or VS Code using `com.routex.Main` as the entry point.

If Maven is installed but not on PATH, you can run it with the full path to `mvn.cmd`, for example:

```powershell
C:\apache-maven-3.9.9\bin\mvn.cmd clean package
C:\apache-maven-3.9.9\bin\mvn.cmd javafx:run
```

If you are using Maven Daemon, the command file is usually `mvnd.cmd`. With your path, run:

```powershell
"C:\Program Files\maven-mvnd-2.0.0-rc-3-windows-amd64\bin\mvnd.cmd" clean package
"C:\Program Files\maven-mvnd-2.0.0-rc-3-windows-amd64\bin\mvnd.cmd" javafx:run
```

If you prefer running the compiled jar or launching from an IDE, use `com.routex.Main` as the entry point.

## Project Structure

- `src/main/java/com/routex/Main.java` - JavaFX application entry point
- `src/main/java/com/routex/ui/RouteXDashboard.java` - Main dashboard UI
- `src/main/java/com/routex/service/` - business logic layer
- `src/main/java/com/routex/dal/` - database access layer
- `src/main/java/com/routex/model/` - domain models
- `src/main/java/com/routex/enums/` - enums used by the domain layer
- `src/main/resources/db/` - database scripts
- `src/main/resources/ui/theme.css` - UI styling

## Notes

- The application uses a single integrated dashboard for all selected use cases.
- Audit logging is handled through the database layer.
- Passwords are stored as SHA-256 hashes in the seed data.
