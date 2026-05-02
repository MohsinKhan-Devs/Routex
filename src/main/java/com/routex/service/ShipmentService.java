package com.routex.service;
import com.routex.enums.ShipmentStatus;
import com.routex.enums.VehicleStatus;
import com.routex.model.Shipment;
import com.routex.dal.AuditLogDAO;
import com.routex.dal.ShipmentDAO;
import com.routex.dal.VehicleDAO;

import java.util.List;

/**
 * Business logic service for UC09 — Update Delivery Status.
 *
 * Business Rules:
 *  - Only the assigned Field Driver may advance a shipment's status.
 *  - Status must follow the allowed lifecycle:
 *    DISPATCHED → IN_TRANSIT → DELIVERED (or CANCELLED at any stage)
 *  - When a shipment is DELIVERED, the vehicle status reverts to AVAILABLE.
 *  - Each status update is attributed to the driver (logged in audit trail).
 *
 * Design Patterns:
 *  - GRASP Controller: single service operation for status transitions.
 *  - State Pattern (implicit): Shipment.canAdvanceTo() encodes the transition rules.
 */
public class ShipmentService {

    private final ShipmentDAO shipmentDAO;
    private final VehicleDAO  vehicleDAO;
    private final AuditLogDAO auditDAO;

    public ShipmentService() {
        this.shipmentDAO = new ShipmentDAO();
        this.vehicleDAO  = new VehicleDAO();
        this.auditDAO    = new AuditLogDAO();
    }

    // -------------------------------------------------------------------------
    // UC09 — Update Delivery Status
    // -------------------------------------------------------------------------

    /**
     * Advances the delivery status of a shipment to the next state.
     *
     * @param shipmentId the UUID of the shipment
     * @param newStatus  the target status (must be a valid next state)
     * @param driverId   the UUID of the Field Driver performing the update
     * @param actorId    same as driverId — used for the audit log
     */
    public void updateDeliveryStatus(String shipmentId, ShipmentStatus newStatus,
                                     String driverId, String actorId)
            throws ShipmentException {

        // 1. Load the shipment
        Shipment shipment = shipmentDAO.findById(shipmentId);
        if (shipment == null)
            throw new ShipmentException("Shipment not found.");

        // 2. Verify the requesting driver is the assigned driver
        if (!shipment.getDriverId().equalsIgnoreCase(driverId))
            throw new ShipmentException("You are not the assigned driver for this shipment.");

        // 3. Validate the state transition (Shipment acts as State machine)
        if (!shipment.canAdvanceTo(newStatus)) {
            throw new ShipmentException(
                "Cannot transition from " + shipment.getStatus().getDisplayName()
                + " to "   + newStatus.getDisplayName()
                + ". Allowed transitions: "
                + allowedNextStates(shipment.getStatus()));
        }

        // 4. Persist the new status
        boolean ok = shipmentDAO.updateStatus(shipmentId, newStatus);
        if (!ok) throw new ShipmentException("Status update failed.");

        // 5. When delivery is complete, release the vehicle
        if (newStatus == ShipmentStatus.DELIVERED || newStatus == ShipmentStatus.CANCELLED) {
            vehicleDAO.updateStatus(shipment.getVehicleId(), VehicleStatus.AVAILABLE);
        }

        // 6. Audit trail
        auditDAO.log(actorId, "SHIPMENT_STATUS_" + newStatus.name(), "Shipment", null);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns all shipments assigned to a specific driver.
     * Used by the Field Driver's delivery status screen.
     */
    public List<Shipment> getShipmentsForDriver(String driverId) {
        return shipmentDAO.findByDriver(driverId);
    }

    /** Returns all shipments — used by the Fleet Dispatcher dashboard. */
    public List<Shipment> getAllShipments() {
        return shipmentDAO.findAll();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private String allowedNextStates(ShipmentStatus current) {
        return switch (current) {
            case DISPATCHED -> "IN_TRANSIT, CANCELLED";
            case IN_TRANSIT -> "DELIVERED, CANCELLED";
            default         -> "None (terminal state)";
        };
    }

    // -------------------------------------------------------------------------
    // Inner Exception Class
    // -------------------------------------------------------------------------

    public static class ShipmentException extends Exception {
        public ShipmentException(String message) { super(message); }
    }
}