package com.routex.dal;
import com.routex.enums.ShipmentStatus;
import com.routex.model.Shipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for the Shipment table (UC06, UC09).
 */
public class ShipmentDAO {

    private final Connection conn;

    public ShipmentDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** Returns all shipments, joined with vehicle plate and driver name. */
    public List<Shipment> findAll() {
        List<Shipment> list = new ArrayList<>();
        String sql = """
            SELECT s.*, v.LicensePlate, u.Name AS DriverName
            FROM Shipment s
            LEFT JOIN Vehicle v ON s.VehicleId = v.VehicleId
            LEFT JOIN Users   u ON s.DriverId  = u.UserId
            ORDER BY s.AssignedAt DESC
            """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentDAO.findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns all shipments assigned to a specific driver.
     * Used by the Field Driver's view in UC09.
     */
    public List<Shipment> findByDriver(String driverId) {
        List<Shipment> list = new ArrayList<>();
        String sql = """
            SELECT s.*, v.LicensePlate, u.Name AS DriverName
            FROM Shipment s
            LEFT JOIN Vehicle v ON s.VehicleId = v.VehicleId
            LEFT JOIN Users   u ON s.DriverId  = u.UserId
            WHERE s.DriverId = ?
            ORDER BY s.AssignedAt DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, driverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentDAO.findByDriver failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Finds a single shipment by its UUID. */
    public Shipment findById(String shipmentId) {
        String sql = """
            SELECT s.*, v.LicensePlate, u.Name AS DriverName
            FROM Shipment s
            LEFT JOIN Vehicle v ON s.VehicleId = v.VehicleId
            LEFT JOIN Users   u ON s.DriverId  = u.UserId
            WHERE s.ShipmentId = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shipmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentDAO.findById failed: " + e.getMessage(), e);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    /**
     * Creates a new Shipment record linking vehicle, driver, and order (UC06).
     * Also inserts the Shipment_Orders_Map entry.
     *
     * @return the generated ShipmentId UUID
     */
    public String save(Shipment shipment, String orderId) throws SQLException {
        String shipmentId = UUID.randomUUID().toString();
        conn.setAutoCommit(false);
        try {
            // 1. Insert Shipment
            String sqlShipment = """
                INSERT INTO Shipment (ShipmentId, VehicleId, DriverId, Status, AssignedAt)
                VALUES (?, ?, ?, ?, GETDATE())
                """;
            try (PreparedStatement ps = conn.prepareStatement(sqlShipment)) {
                ps.setString(1, shipmentId);
                ps.setString(2, shipment.getVehicleId());
                ps.setString(3, shipment.getDriverId());
                ps.setString(4, ShipmentStatus.DISPATCHED.name());
                ps.executeUpdate();
            }

            // 2. Insert Shipment_Orders_Map
            String sqlMap = """
                INSERT INTO Shipment_Orders_Map (ShipmentId, OrderId, DeliverySequence)
                VALUES (?, ?, 1)
                """;
            try (PreparedStatement ps = conn.prepareStatement(sqlMap)) {
                ps.setString(1, shipmentId);
                ps.setString(2, orderId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return shipmentId;
    }

    /**
     * Advances the shipment status (UC09).
     * Sets CompletedAt when status becomes DELIVERED or CANCELLED.
     */
    public boolean updateStatus(String shipmentId, ShipmentStatus newStatus) {
        String sql;
        if (newStatus == ShipmentStatus.DELIVERED || newStatus == ShipmentStatus.CANCELLED) {
            sql = "UPDATE Shipment SET Status=?, CompletedAt=GETDATE() WHERE ShipmentId=?";
        } else {
            sql = "UPDATE Shipment SET Status=? WHERE ShipmentId=?";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, shipmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentDAO.updateStatus failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private Shipment mapRow(ResultSet rs) throws SQLException {
        Shipment s = new Shipment();
        s.setShipmentId(rs.getString("ShipmentId"));
        s.setVehicleId(rs.getString("VehicleId"));
        s.setVehicleLicensePlate(rs.getString("LicensePlate"));
        s.setDriverId(rs.getString("DriverId"));
        s.setDriverName(rs.getString("DriverName"));
        s.setStatus(ShipmentStatus.fromString(rs.getString("Status")));
        s.setAssignedAt(rs.getTimestamp("AssignedAt").toLocalDateTime());
        Timestamp completed = rs.getTimestamp("CompletedAt");
        if (completed != null) s.setCompletedAt(completed.toLocalDateTime());
        return s;
    }
}