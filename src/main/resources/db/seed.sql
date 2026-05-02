USE RouteX_DB;
GO

DECLARE @AdminId UNIQUEIDENTIFIER = NEWID();
DECLARE @ManagerId UNIQUEIDENTIFIER = NEWID();
DECLARE @DispatcherId UNIQUEIDENTIFIER = NEWID();
DECLARE @DriverId1 UNIQUEIDENTIFIER = NEWID();
DECLARE @DriverId2 UNIQUEIDENTIFIER = NEWID();

INSERT INTO Users (UserId, Name, Email, PasswordHash, Role, Status) VALUES
(@AdminId, 'Sarah Jenkins', 's.jenkins@routex.com', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', 'SYSTEM_ADMIN', 'ACTIVE'),
(@ManagerId, 'Marcus Thorne', 'm.thorne@routex.com', 'e8392925a98c9c22795d1fc5d0dfee5b9a6943f6b768ec5a2a0c077e5ed119cf', 'INVENTORY_MANAGER', 'ACTIVE'),
(@DispatcherId, 'Elena Rodriguez', 'e.rodriguez@routex.com', 'b2f3fa01872d158803f8ab23bf1ef026f180ec9382565af1dad8c53b4fa4a54b', 'FLEET_DISPATCHER', 'ACTIVE'),
(@DriverId1, 'James Logan Howlett', 'j.howlett@routex.com', 'a3593b1af260573f0c0c58dd3f979656421440673d3b797abe394d6ac56d9c25', 'FIELD_DRIVER', 'ACTIVE'),
(@DriverId2, 'Riley Vance', 'r.vance@routex.com', '5ff49a045004d9c428c5432920669ea04d95b8b0a75fc77f4d459a31b2a71e3c', 'FIELD_DRIVER', 'ACTIVE');

DECLARE @WhNorth UNIQUEIDENTIFIER = NEWID();
DECLARE @WhSouth UNIQUEIDENTIFIER = NEWID();

INSERT INTO Warehouse (WarehouseId, Name, LocationAddress, MaxCapacity) VALUES
(@WhNorth, 'North Hub - Logistics Park', '100 Industrial Way, Chicago, IL', 5000),
(@WhSouth, 'South Coast Distribution', '452 Ocean View Dr, Savannah, GA', 3500);

DECLARE @ItemTech UNIQUEIDENTIFIER = NEWID();
DECLARE @ItemMed UNIQUEIDENTIFIER = NEWID();

INSERT INTO InventoryItem (ItemId, SKU, Name, Quantity, ExpiryDate, WarehouseId, ReorderThreshold) VALUES
(@ItemTech, 'SKU-7700-X', 'Enterprise Server Rack', 8, '2030-01-01', @WhNorth, 10),
(@ItemMed, 'SKU-MED-02', 'Insulin Cooling Units', 40, '2027-06-15', @WhSouth, 50);

DECLARE @Order1 UNIQUEIDENTIFIER = NEWID();
DECLARE @Order2 UNIQUEIDENTIFIER = NEWID();

INSERT INTO ShipmentOrder (OrderId, ItemId, RequiredQty, DestinationAddress, Priority, Status, ExpectedDeliveryDate) VALUES
(@Order1, @ItemTech, 2, 'Tech Park Phase II, Seattle, WA', 'HIGH', 'PENDING_APPROVAL', '2026-05-10'),
(@Order2, @ItemMed, 15, 'St Marys General Hospital, FL', 'CRITICAL', 'APPROVED', '2026-05-04');

DECLARE @Truck1 UNIQUEIDENTIFIER = NEWID();
DECLARE @Van1 UNIQUEIDENTIFIER = NEWID();

INSERT INTO Vehicle (VehicleId, LicensePlate, Capacity, Status, CurrentLocation, Mileage) VALUES
(@Truck1, 'RTX-9981', 10000, 'AVAILABLE', 'Interstate 95-N', 12450),
(@Van1, 'RTX-2240', 2500, 'AVAILABLE', 'South Coast Distribution Hub', 4200);

INSERT INTO AuditLog (ActorId, Action, EntityType, IpAddress) VALUES
(@AdminId, 'SEED_DATA_INIT', 'SYSTEM', '127.0.0.1');
GO
