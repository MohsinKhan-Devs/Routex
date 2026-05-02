package com.routex.dal;
import com.routex.enums.VehicleStatus;
import com.routex.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the Vehicle table (UC06, UC10).
 */
public class VehicleDAO {

    private final Connection conn;

    public VehicleDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** Returns all vehicles. */
    public List<Vehicle> findAll() {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM Vehicle ORDER BY LicensePlate";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("VehicleDAO.findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns only AVAILABLE vehicles for assignment suggestions (UC06).
     * MAINTENANCE_HOLD and IN_TRANSIT vehicles are excluded automatically.
     */
    public List<Vehicle> findAvailable() {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM Vehicle WHERE Status='AVAILABLE' ORDER BY Capacity DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("VehicleDAO.findAvailable failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Finds a single vehicle by its UUID. */
    public Vehicle findById(String vehicleId) {
        String sql = "SELECT * FROM Vehicle WHERE VehicleId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("VehicleDAO.findById failed: " + e.getMessage(), e);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    /**
     * Updates the vehicle's status.
     * Used when transitioning to IN_TRANSIT (UC06) or MAINTENANCE_HOLD (UC10).
     */
    public boolean updateStatus(String vehicleId, VehicleStatus newStatus) {
        String sql = "UPDATE Vehicle SET Status=? WHERE VehicleId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, vehicleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("VehicleDAO.updateStatus failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private Vehicle mapRow(ResultSet rs) throws SQLException {
        return new Vehicle(
            rs.getString("VehicleId"),
            rs.getString("LicensePlate"),
            rs.getInt("Capacity"),
            VehicleStatus.fromString(rs.getString("Status")),
            rs.getString("CurrentLocation"),
            rs.getInt("Mileage")
        );
    }
}