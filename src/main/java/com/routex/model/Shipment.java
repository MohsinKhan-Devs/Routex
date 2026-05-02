package com.routex.model;
import com.routex.enums.ShipmentStatus;
import java.time.LocalDateTime;

/**
 * A Shipment links an approved ShipmentOrder to a Vehicle and Driver (UC06, UC09).
 * The Field Driver advances the Shipment through its status lifecycle (UC09).
 *
 * OOP Principles:
 *  - Encapsulation: status transitions validated by canAdvanceTo()
 */
public class Shipment {

    private String         shipmentId;
    private String         vehicleId;
    private String         vehicleLicensePlate;  // Denormalised for display
    private String         driverId;
    private String         driverName;            // Denormalised for display
    private ShipmentStatus status;
    private LocalDateTime  assignedAt;
    private LocalDateTime  completedAt;

    public Shipment() {}

    public Shipment(String shipmentId, String vehicleId, String driverId,
                    ShipmentStatus status, LocalDateTime assignedAt) {
        this.shipmentId = shipmentId;
        this.vehicleId  = vehicleId;
        this.driverId   = driverId;
        this.status     = status;
        this.assignedAt = assignedAt;
    }

    // -------------------------------------------------------------------------
    // Business Logic
    // -------------------------------------------------------------------------

    /**
     * Validates whether a status transition is allowed in the five-state
     * lifecycle defined in UC09:
     *   DISPATCHED → IN_TRANSIT → DELIVERED (terminal)
     *   Any non-terminal state → CANCELLED  (terminal)
     */
    public boolean canAdvanceTo(ShipmentStatus next) {
        return switch (status) {
            case DISPATCHED -> next == ShipmentStatus.IN_TRANSIT
                            || next == ShipmentStatus.CANCELLED;
            case IN_TRANSIT -> next == ShipmentStatus.DELIVERED
                            || next == ShipmentStatus.CANCELLED;
            default         -> false;  // DELIVERED and CANCELLED are terminal
        };
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getShipmentId()                    { return shipmentId; }
    public void   setShipmentId(String shipmentId)   { this.shipmentId = shipmentId; }

    public String getVehicleId()                  { return vehicleId; }
    public void   setVehicleId(String vehicleId)  { this.vehicleId = vehicleId; }

    public String getVehicleLicensePlate()                           { return vehicleLicensePlate; }
    public void   setVehicleLicensePlate(String vehicleLicensePlate){ this.vehicleLicensePlate = vehicleLicensePlate; }

    public String getDriverId()                  { return driverId; }
    public void   setDriverId(String driverId)   { this.driverId = driverId; }

    public String getDriverName()                    { return driverName; }
    public void   setDriverName(String driverName)   { this.driverName = driverName; }

    public ShipmentStatus getStatus()                        { return status; }
    public void           setStatus(ShipmentStatus status)   { this.status = status; }

    public LocalDateTime getAssignedAt()                   { return assignedAt; }
    public void          setAssignedAt(LocalDateTime dt)   { this.assignedAt = dt; }

    public LocalDateTime getCompletedAt()                  { return completedAt; }
    public void          setCompletedAt(LocalDateTime dt)  { this.completedAt = dt; }

    @Override
    public String toString() {
        return "Shipment[" + shipmentId.substring(0, 8) + "] " + vehicleLicensePlate + " - " + status.getDisplayName();
    }
}