package com.routex.service;
import com.routex.enums.ShipmentStatus;
import com.routex.enums.VehicleStatus;
import com.routex.model.Shipment;
import com.routex.model.User;
import com.routex.model.Vehicle;
import com.routex.model.VehicleIssue;
import com.routex.dal.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic service for:
 *  - UC06 — Assign Vehicle to Shipment
 *  - UC10 — Report Vehicle Issue
 *
 * Business Rules (UC06):
 *  - Only AVAILABLE vehicles may be assigned.
 *  - MAINTENANCE_HOLD vehicles are filtered out of suggestions.
 *  - On assignment: vehicle status → IN_TRANSIT; order status → DISPATCHED.
 *
 * Business Rules (UC10):
 *  - Issue capture: description, GPS location, category all required.
 *  - On save: vehicle status → MAINTENANCE_HOLD atomically.
 *  - A MaintenanceRecord is created automatically.
 *
 * Design Patterns:
 *  - GRASP Controller: central handler for vehicle-related use cases.
 *  - GoF Facade: hides the multi-DAO transaction behind a single method call.
 */
public class VehicleService {

    private final VehicleDAO       vehicleDAO;
    private final ShipmentDAO      shipmentDAO;
    private final ShipmentOrderDAO orderDAO;
    private final VehicleIssueDAO  issueDAO;
    private final AuditLogDAO      auditDAO;

    public VehicleService() {
        this.vehicleDAO  = new VehicleDAO();
        this.shipmentDAO = new ShipmentDAO();
        this.orderDAO    = new ShipmentOrderDAO();
        this.issueDAO    = new VehicleIssueDAO();
        this.auditDAO    = new AuditLogDAO();
    }

    // -------------------------------------------------------------------------
    // UC06 — Assign Vehicle to Shipment
    // -------------------------------------------------------------------------

    /**
     * Returns vehicles eligible for assignment.
     * MAINTENANCE_HOLD vehicles are excluded per UC06 business rule.
     * The list is ordered by capacity (largest first) to aid dispatcher decisions.
     */
    public List<Vehicle> getEligibleVehicles() {
        return vehicleDAO.findAvailable();
    }

    /** Returns all vehicles (for the dispatcher dashboard overview). */
    public List<Vehicle> getAllVehicles() {
        return vehicleDAO.findAll();
    }

    /** Returns all active users who have FIELD_DRIVER role. */
    public List<User> getActiveDrivers() {
        return new UserDAO().findAllDrivers();
    }

    /**
     * Assigns a vehicle and driver to an approved shipment order.
     *
     * Steps:
     *  1. Verify the vehicle is AVAILABLE.
     *  2. Create a Shipment record (status = DISPATCHED).
     *  3. Link the order to the shipment in Shipment_Orders_Map.
     *  4. Set the vehicle status to IN_TRANSIT.
     *  5. Set the order status to DISPATCHED.
     *
     * @param vehicleId the UUID of the selected vehicle
     * @param orderId   the UUID of the approved shipment order
     * @param driverId  the UUID of the assigned Field Driver
     * @param actorId   the Fleet Dispatcher's UserId
     * @return the generated ShipmentId
     */
    public String assignVehicle(String vehicleId, String orderId, String driverId,
                                String actorId) throws VehicleException {
        // 1. Validate vehicle eligibility
        Vehicle vehicle = vehicleDAO.findById(vehicleId);
        if (vehicle == null)
            throw new VehicleException("Vehicle not found.");
        if (!vehicle.isEligibleForAssignment())
            throw new VehicleException("Vehicle " + vehicle.getLicensePlate()
                + " is not available. Current status: " + vehicle.getStatus().getDisplayName());

        // 2. Build Shipment domain object
        Shipment shipment = new Shipment();
        shipment.setVehicleId(vehicleId);
        shipment.setDriverId(driverId);
        shipment.setStatus(ShipmentStatus.DISPATCHED);

        try {
            // 3. Persist shipment + map (transactional in ShipmentDAO)
            String shipmentId = shipmentDAO.save(shipment, orderId);

            // 4. Transition vehicle to IN_TRANSIT
            vehicleDAO.updateStatus(vehicleId, VehicleStatus.IN_TRANSIT);

            // 5. Mark the order as DISPATCHED
            orderDAO.markDispatched(orderId);

            auditDAO.log(actorId, "VEHICLE_ASSIGNED", "Shipment", null);
            return shipmentId;

        } catch (SQLException e) {
            throw new VehicleException("Assignment failed due to a database error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // UC10 — Report Vehicle Issue
    // -------------------------------------------------------------------------

    /**
     * Records a vehicle defect or breakdown and places the vehicle in
     * MAINTENANCE_HOLD.  A MaintenanceRecord is also created atomically.
     *
     * @param vehicleId   the UUID of the affected vehicle
     * @param driverId    the UUID of the Field Driver reporting the issue
     * @param description a free-text description of the problem
     * @param category    issue category (e.g., ENGINE, TYRE, ELECTRICAL)
     * @param gpsLocation the GPS coordinates at time of report
     * @param actorId     the Field Driver's UserId
     * @return the generated IssueId
     */
    public String reportIssue(String vehicleId, String driverId,
                              String description, String category,
                              String gpsLocation, String actorId) throws VehicleException {
        // Validate inputs
        if (description == null || description.isBlank())
            throw new VehicleException("Issue description cannot be empty.");
        if (category == null || category.isBlank())
            throw new VehicleException("Issue category is required.");
        if (gpsLocation == null || gpsLocation.isBlank())
            throw new VehicleException("GPS location is required.");

        Vehicle vehicle = vehicleDAO.findById(vehicleId);
        if (vehicle == null)
            throw new VehicleException("Vehicle not found.");

        VehicleIssue issue = new VehicleIssue(null, vehicleId, driverId,
                                              description, category, gpsLocation);
        try {
            // Saves issue, puts vehicle in MAINTENANCE_HOLD, creates MaintenanceRecord
            String issueId = issueDAO.save(issue);
            auditDAO.log(actorId, "VEHICLE_ISSUE_REPORTED", "VehicleIssue", null);
            return issueId;
        } catch (SQLException e) {
            throw new VehicleException("Failed to report issue: " + e.getMessage());
        }
    }

    /** Returns all vehicle issues (for Fleet Dispatcher overview). */
    public List<VehicleIssue> getAllIssues() {
        return issueDAO.findAll();
    }

    // -------------------------------------------------------------------------
    // Inner Exception Class
    // -------------------------------------------------------------------------

    public static class VehicleException extends Exception {
        public VehicleException(String message) { super(message); }
    }
}