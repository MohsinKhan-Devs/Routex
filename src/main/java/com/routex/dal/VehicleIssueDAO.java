package com.routex.dal;
import com.routex.model.VehicleIssue;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for VehicleIssue and MaintenanceRecord tables (UC10).
 */
public class VehicleIssueDAO {

    private final Connection conn;

    public VehicleIssueDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** Returns all vehicle issues, joined with vehicle plate and driver name. */
    public List<VehicleIssue> findAll() {
        List<VehicleIssue> list = new ArrayList<>();
        String sql = """
            SELECT vi.*, v.LicensePlate, u.Name AS DriverName
            FROM VehicleIssue vi
            LEFT JOIN Vehicle v ON vi.VehicleId = v.VehicleId
            LEFT JOIN Users   u ON vi.DriverId  = u.UserId
            ORDER BY vi.Timestamp DESC
            """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("VehicleIssueDAO.findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Returns all unresolved issues for a specific vehicle. */
    public List<VehicleIssue> findByVehicle(String vehicleId) {
        List<VehicleIssue> list = new ArrayList<>();
        String sql = """
            SELECT vi.*, v.LicensePlate, u.Name AS DriverName
            FROM VehicleIssue vi
            LEFT JOIN Vehicle v ON vi.VehicleId = v.VehicleId
            LEFT JOIN Users   u ON vi.DriverId  = u.UserId
            WHERE vi.VehicleId = ?
            ORDER BY vi.Timestamp DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("VehicleIssueDAO.findByVehicle failed: " + e.getMessage(), e);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    /**
     * Saves a new issue report and atomically sets the vehicle status to
     * MAINTENANCE_HOLD and creates a MaintenanceRecord (UC10).
     *
     * @return the generated IssueId UUID
     */
    public String save(VehicleIssue issue) throws SQLException {
        String issueId = UUID.randomUUID().toString();
        conn.setAutoCommit(false);
        try {
            // 1. Insert VehicleIssue
            String sqlIssue = """
                INSERT INTO VehicleIssue
                  (IssueId, VehicleId, DriverId, Description, Category, GpsLocation, Timestamp, IsResolved)
                VALUES (?, ?, ?, ?, ?, ?, GETDATE(), 0)
                """;
            try (PreparedStatement ps = conn.prepareStatement(sqlIssue)) {
                ps.setString(1, issueId);
                ps.setString(2, issue.getVehicleId());
                ps.setString(3, issue.getDriverId());
                ps.setString(4, issue.getDescription());
                ps.setString(5, issue.getCategory());
                ps.setString(6, issue.getGpsLocation());
                ps.executeUpdate();
            }

            // 2. Transition vehicle to MAINTENANCE_HOLD
            String sqlVehicle = "UPDATE Vehicle SET Status='MAINTENANCE_HOLD' WHERE VehicleId=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlVehicle)) {
                ps.setString(1, issue.getVehicleId());
                ps.executeUpdate();
            }

            // 3. Create a blank MaintenanceRecord (to be filled by maintenance team)
            String sqlMaint = """
                INSERT INTO MaintenanceRecord (IssueId, ResolutionNotes)
                VALUES (?, 'Pending resolution')
                """;
            try (PreparedStatement ps = conn.prepareStatement(sqlMaint)) {
                ps.setString(1, issueId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return issueId;
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private VehicleIssue mapRow(ResultSet rs) throws SQLException {
        VehicleIssue vi = new VehicleIssue();
        vi.setIssueId(rs.getString("IssueId"));
        vi.setVehicleId(rs.getString("VehicleId"));
        vi.setVehicleLicensePlate(rs.getString("LicensePlate"));
        vi.setDriverId(rs.getString("DriverId"));
        vi.setDriverName(rs.getString("DriverName"));
        vi.setDescription(rs.getString("Description"));
        vi.setCategory(rs.getString("Category"));
        vi.setGpsLocation(rs.getString("GpsLocation"));
        vi.setTimestamp(rs.getTimestamp("Timestamp").toLocalDateTime());
        vi.setResolved(rs.getBoolean("IsResolved"));
        return vi;
    }
}