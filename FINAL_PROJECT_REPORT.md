# RouteX Final Project Report

## 1. Project Overview
RouteX is an inventory and fleet management system built for a three-tier architecture. The system centralizes authentication, inventory stock handling, shipment order generation and approval, vehicle assignment, delivery updates, vehicle issue reporting, and user management.

## 2. Selected Use Cases
The project implements the following eight use cases:
1. UC01 - Login / Authenticate
2. UC02 - Manage Inventory Stock
3. UC04 - Generate Shipment Order
4. UC05 - Approve / Reject Shipment Order
5. UC06 - Assign Vehicle to Shipment
6. UC09 - Update Delivery Status
7. UC10 - Report Vehicle Issue
8. UC11 - Manage User Accounts

These are intentionally selected to satisfy the group requirement with a mix of foundational and unique operations.

## 3. Architectural Style
The application follows a 3-tier architecture:
1. Presentation Layer: JavaFX dashboard in [src/main/java/com/routex/Main.java](src/main/java/com/routex/Main.java) and [src/main/java/com/routex/ui/RouteXDashboard.java](src/main/java/com/routex/ui/RouteXDashboard.java)
2. Business Layer: service classes in [src/main/java/com/routex/service](src/main/java/com/routex/service)
3. Data Layer: DAO classes in [src/main/java/com/routex/dal](src/main/java/com/routex/dal)

This style was chosen because it separates UI concerns from business rules and persistence, which improves maintainability, testability, and demonstration clarity.

## 4. Design Principles and Patterns
The codebase uses the following principles and patterns:
1. GRASP Controller: each service class coordinates one or more use cases.
2. DAO Pattern: all SQL access is isolated inside DAO classes.
3. Encapsulation: domain models hold state and expose controlled behavior through methods.
4. Utility Pattern: password hashing is centralized in [src/main/java/com/routex/util/PasswordUtil.java](src/main/java/com/routex/util/PasswordUtil.java).
5. Singleton: [src/main/java/com/routex/dal/DatabaseConnection.java](src/main/java/com/routex/dal/DatabaseConnection.java) manages one shared database connection instance.
6. State-like validation: shipment and order lifecycle rules are enforced in domain methods such as canAdvanceTo() and isApprovable().

## 5. Business Logic Summary
The business layer contains the main operational logic:
1. AuthService handles login validation, lockout, and audit logging.
2. InventoryService manages inventory create, update, delete, and threshold-based lookup.
3. ShipmentOrderService generates orders manually or from low stock, and approves or rejects orders.
4. VehicleService assigns vehicles to shipments and records vehicle issues.
5. ShipmentService updates delivery status with lifecycle validation.
6. UserManagementService manages account creation, updates, deactivation, and deletion.

## 6. Database Layer
The SQL Server schema is stored in:
1. [src/main/resources/db/schema.sql](src/main/resources/db/schema.sql)
2. [src/main/resources/db/seed.sql](src/main/resources/db/seed.sql)

Main tables include Users, Warehouse, InventoryItem, ShipmentOrder, Vehicle, Shipment, Shipment_Orders_Map, VehicleIssue, MaintenanceRecord, and AuditLog.

## 7. User Interface
The user interface is a single JavaFX application.

Main features:
1. Login screen for UC01.
2. Role-based dashboard tabs.
3. Integrated actions for inventory, orders, dispatch, delivery, issues, and user accounts.
4. Styled interface using [src/main/resources/ui/theme.css](src/main/resources/ui/theme.css).

## 8. Seed Data and Demo Accounts
The database seed script includes sample accounts and operational data.

Seed login accounts:
1. System Admin: s.jenkins@routex.com / Admin@123
2. Inventory Manager: m.thorne@routex.com / Manager@123
3. Fleet Dispatcher: e.rodriguez@routex.com / Dispatcher@123
4. Field Driver: j.howlett@routex.com / Driver1@123

## 9. Key Classes
Important implementation files:
1. [src/main/java/com/routex/Main.java](src/main/java/com/routex/Main.java)
2. [src/main/java/com/routex/ui/RouteXDashboard.java](src/main/java/com/routex/ui/RouteXDashboard.java)
3. [src/main/java/com/routex/service/AuthService.java](src/main/java/com/routex/service/AuthService.java)
4. [src/main/java/com/routex/service/InventoryService.java](src/main/java/com/routex/service/InventoryService.java)
5. [src/main/java/com/routex/service/ShipmentOrderService.java](src/main/java/com/routex/service/ShipmentOrderService.java)
6. [src/main/java/com/routex/service/ShipmentService.java](src/main/java/com/routex/service/ShipmentService.java)
7. [src/main/java/com/routex/service/UserManagementService.java](src/main/java/com/routex/service/UserManagementService.java)
8. [src/main/java/com/routex/service/VehicleService.java](src/main/java/com/routex/service/VehicleService.java)
9. [src/main/java/com/routex/dal](src/main/java/com/routex/dal)
10. [src/main/java/com/routex/model](src/main/java/com/routex/model)

## 10. Submission Notes
The project is now organized as one integrated system rather than separate demonstrations. This supports the requirement that all functionality must be shown in one application.

For final evaluation, be ready to explain:
1. Why 3-tier architecture was chosen.
2. How each selected use case maps to a service and DAO.
3. How OOP principles are used in the domain models.
4. How audit logging and database persistence are handled.
5. How the UI and business layers remain connected.
