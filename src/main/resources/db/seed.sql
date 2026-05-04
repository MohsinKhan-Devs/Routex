USE RouteX_DB;
GO

-- =====================================================================
-- 1. CLEAR EXISTING DATA
-- Deleting in reverse dependency order to avoid Foreign Key constraint errors
-- =====================================================================
DELETE FROM AuditLog;
DELETE FROM MaintenanceRecord;
DELETE FROM VehicleIssue;
DELETE FROM Shipment_Orders_Map;
DELETE FROM Shipment;
DELETE FROM ShipmentOrder;
DELETE FROM InventoryItem;
DELETE FROM Warehouse;
DELETE FROM Vehicle;
DELETE FROM Users;
GO

-- =====================================================================
-- 2. SEED USERS
-- =====================================================================
DECLARE @AdminId UNIQUEIDENTIFIER = NEWID();
DECLARE @ManagerId UNIQUEIDENTIFIER = NEWID();
DECLARE @DispatcherId UNIQUEIDENTIFIER = NEWID();
DECLARE @DriverId1 UNIQUEIDENTIFIER = NEWID();
DECLARE @DriverId2 UNIQUEIDENTIFIER = NEWID();

-- SHA-256 Hash for '123456'
DECLARE @DefaultPasswordHash NVARCHAR(256) = '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92';

INSERT INTO Users (UserId, Name, Email, PasswordHash, Role, Status) VALUES
(@AdminId, 'Ali Khan', 'ali.khan@routex.pk', @DefaultPasswordHash, 'SYSTEM_ADMIN', 'ACTIVE'),
(@ManagerId, 'Fatima Ahmed', 'fatima.ahmed@routex.pk', @DefaultPasswordHash, 'INVENTORY_MANAGER', 'ACTIVE'),
(@DispatcherId, 'Zainab Tariq', 'zainab.tariq@routex.pk', @DefaultPasswordHash, 'FLEET_DISPATCHER', 'ACTIVE'),
(@DriverId1, 'Muhammad Usman', 'm.usman@routex.pk', @DefaultPasswordHash, 'FIELD_DRIVER', 'ACTIVE'),
(@DriverId2, 'Bilal Raza', 'b.raza@routex.pk', @DefaultPasswordHash, 'FIELD_DRIVER', 'ACTIVE');

-- =====================================================================
-- 3. SEED WAREHOUSES
-- =====================================================================
DECLARE @WhKarachi UNIQUEIDENTIFIER = NEWID();
DECLARE @WhLahore UNIQUEIDENTIFIER = NEWID();
DECLARE @WhIslamabad UNIQUEIDENTIFIER = NEWID();

INSERT INTO Warehouse (WarehouseId, Name, LocationAddress, MaxCapacity) VALUES
(@WhKarachi, 'Karachi Port Hub', 'Plot 14, SITE Area, Karachi, Sindh', 10000),
(@WhLahore, 'Lahore Central Distribution', 'Sundar Industrial Estate, Lahore, Punjab', 8000),
(@WhIslamabad, 'Islamabad North Transit', 'Sector I-9/2, Industrial Area, Islamabad', 5000);

-- =====================================================================
-- 4. SEED INVENTORY ITEMS
-- =====================================================================
DECLARE @ItemSurgical UNIQUEIDENTIFIER = NEWID();
DECLARE @ItemTextile UNIQUEIDENTIFIER = NEWID();
DECLARE @ItemElectronics UNIQUEIDENTIFIER = NEWID();

INSERT INTO InventoryItem (ItemId, SKU, Name, Quantity, ExpiryDate, WarehouseId, ReorderThreshold) VALUES
(@ItemSurgical, 'SKU-MED-KHI-01', 'Surgical Masks (Box of 50)', 1500, '2028-12-31', @WhKarachi, 200),
(@ItemTextile, 'SKU-TEX-LHR-99', 'Premium Cotton Bales', 300, '2030-01-01', @WhLahore, 50),
(@ItemElectronics, 'SKU-ELC-ISB-55', 'Office Laptops Core i7', 120, '2035-06-15', @WhIslamabad, 20);

-- =====================================================================
-- 5. SEED SHIPMENT ORDERS
-- =====================================================================
DECLARE @Order1 UNIQUEIDENTIFIER = NEWID();
DECLARE @Order2 UNIQUEIDENTIFIER = NEWID();
DECLARE @Order3 UNIQUEIDENTIFIER = NEWID();

INSERT INTO ShipmentOrder (OrderId, ItemId, RequiredQty, DestinationAddress, Priority, Status, ExpectedDeliveryDate) VALUES
(@Order1, @ItemSurgical, 50, 'Aga Khan University Hospital, Stadium Road, Karachi', 'CRITICAL', 'APPROVED', '2026-05-10'),
(@Order2, @ItemTextile, 20, 'Faisalabad Industrial Area, Faisalabad', 'MEDIUM', 'PENDING_APPROVAL', '2026-05-15'),
(@Order3, @ItemElectronics, 15, 'Software Technology Park, I-8 Markaz, Islamabad', 'HIGH', 'APPROVED', '2026-05-08');

-- =====================================================================
-- 6. SEED VEHICLES
-- =====================================================================
DECLARE @Truck1 UNIQUEIDENTIFIER = NEWID();
DECLARE @Van1 UNIQUEIDENTIFIER = NEWID();
DECLARE @Truck2 UNIQUEIDENTIFIER = NEWID();

INSERT INTO Vehicle (VehicleId, LicensePlate, Capacity, Status, CurrentLocation, Mileage) VALUES
(@Truck1, 'KHI-4521', 15000, 'AVAILABLE', 'Super Highway (M-9), Karachi', 45200),
(@Van1, 'LHR-9082', 3000, 'AVAILABLE', 'Ferozepur Road, Lahore', 12500),
(@Truck2, 'ISB-1122', 12000, 'IN_TRANSIT', 'Kashmir Highway, Islamabad', 8900);

-- =====================================================================
-- 7. SEED SHIPMENTS & MAPS (Optional Active Deliveries)
-- =====================================================================
DECLARE @Shipment1 UNIQUEIDENTIFIER = NEWID();
DECLARE @Shipment2 UNIQUEIDENTIFIER = NEWID();

INSERT INTO Shipment (ShipmentId, VehicleId, DriverId, Status) VALUES
(@Shipment1, @Truck1, @DriverId1, 'DISPATCHED'),
(@Shipment2, @Truck2, @DriverId2, 'IN_TRANSIT');

INSERT INTO Shipment_Orders_Map (ShipmentId, OrderId, DeliverySequence) VALUES
(@Shipment1, @Order1, 1),
(@Shipment2, @Order3, 1);

-- =====================================================================
-- 8. SEED VEHICLE ISSUES & MAINTENANCE RECORDS
-- =====================================================================
DECLARE @Issue1 UNIQUEIDENTIFIER = NEWID();
DECLARE @Issue2 UNIQUEIDENTIFIER = NEWID();

INSERT INTO VehicleIssue (IssueId, VehicleId, DriverId, Description, Category, GpsLocation, Timestamp, IsResolved) VALUES
(@Issue1, @Truck1, @DriverId1, 'Engine overheating during incline', 'Engine', 'Salt Range, Motorway M-2', GETDATE() - 5, 1),
(@Issue2, @Van1, @DriverId2, 'Flat tire on front left', 'Tires', 'Clifton Block 5, Karachi', GETDATE() - 1, 0);

DECLARE @Maintenance1 UNIQUEIDENTIFIER = NEWID();

INSERT INTO MaintenanceRecord (RecordId, IssueId, ResolvedDate, ResolutionNotes, RepairCost) VALUES
(@Maintenance1, @Issue1, GETDATE() - 4, 'Replaced radiator coolant and checked water pump.', 15500.00);

GO