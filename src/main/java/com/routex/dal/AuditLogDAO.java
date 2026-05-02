package com.routex.dal;
import java.sql.*;

/**
 * Data Access Object for the AuditLog table (UC12).
 * All system-altering actions are written here for compliance tracing.
 */
public class AuditLogDAO {

    private final Connection conn;

    public AuditLogDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Writes a single audit entry.
     *
     * @param actorId    UUID of the user performing the action
     * @param action     Short action label, e.g. "USER_LOGIN", "ORDER_APPROVED"
     * @param entityType The DB table / domain entity affected, e.g. "ShipmentOrder"
     * @param ipAddress  IP address of the client (may be null in desktop app)
     */
    public void log(String actorId, String action, String entityType, String ipAddress) {
        String sql = """
            INSERT INTO AuditLog (ActorId, Action, EntityType, IpAddress, Timestamp)
            VALUES (?, ?, ?, ?, GETDATE())
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actorId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setString(4, ipAddress != null ? ipAddress : "DESKTOP");
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit logging failure must NOT crash the application
            System.err.println("[AUDIT] Failed to write log entry: " + e.getMessage());
        }
    }
}