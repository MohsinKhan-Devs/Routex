package com.routex.dal;
import com.routex.enums.UserRole;
import com.routex.enums.UserStatus;
import com.routex.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for the Users table.
 * All SQL interaction for user records is isolated in this class.
 *
 * Design Pattern: DAO (GoF Data Access Object)
 *   - Decouples persistence logic from the business layer.
 *   - Services call DAO methods; DAOs speak SQL.
 */
public class UserDAO {

    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /**
     * Finds a user by their email address.
     * Used by AuthService during login (UC01).
     */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE Email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findByEmail failed: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds a user by their UUID primary key.
     * Used when loading a driver for shipment display.
     */
    public User findById(String userId) {
        String sql = "SELECT * FROM Users WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findById failed: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Returns all users — used by the System Admin in UC11.
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users ORDER BY Name";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findAll failed: " + e.getMessage(), e);
        }
        return users;
    }

    /**
     * Returns all users with the FIELD_DRIVER role.
     * Used by VehicleAssignmentController to populate the driver combo box.
     */
    public List<User> findAllDrivers() {
        List<User> drivers = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE Role = 'FIELD_DRIVER' AND Status = 'ACTIVE' ORDER BY Name";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) drivers.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findAllDrivers failed: " + e.getMessage(), e);
        }
        return drivers;
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    /**
     * Inserts a new user record (UC11 — Create).
     */
    public boolean save(User user) {
        String sql = """
            INSERT INTO Users (UserId, Name, Email, PasswordHash, Role, Status, FailedLoginAttempts)
            VALUES (?, ?, ?, ?, ?, ?, 0)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getStatus().name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.save failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing user's name, email, role, and status (UC11 — Edit).
     */
    public boolean update(User user) {
        String sql = """
            UPDATE Users SET Name=?, Email=?, Role=?, Status=?
            WHERE UserId=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getStatus().name());
            ps.setString(5, user.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Hard-deletes a user record (UC11 — Delete).
     * Business logic must verify that at least one SYSTEM_ADMIN remains.
     */
    public boolean delete(String userId) {
        String sql = "DELETE FROM Users WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.delete failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the failed login attempts count and, if needed, lock status.
     * Called by AuthService after each failed login (UC01).
     */
    public boolean updateLoginAttempts(String userId, int failedAttempts, UserStatus status) {
        String sql = "UPDATE Users SET FailedLoginAttempts=?, Status=? WHERE UserId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, failedAttempts);
            ps.setString(2, status.name());
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.updateLoginAttempts failed: " + e.getMessage(), e);
        }
    }

    /**
     * Records the timestamp of a successful login and resets failed counter.
     */
    public boolean recordSuccessfulLogin(String userId) {
        String sql = """
            UPDATE Users SET FailedLoginAttempts=0, LastLoginDate=GETDATE(), Status='ACTIVE'
            WHERE UserId=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.recordSuccessfulLogin failed: " + e.getMessage(), e);
        }
    }

    /**
     * Counts active SYSTEM_ADMIN accounts — used to prevent deleting the last one.
     */
    public int countActiveAdmins() {
        String sql = "SELECT COUNT(*) FROM Users WHERE Role='SYSTEM_ADMIN' AND Status='ACTIVE'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.countActiveAdmins failed: " + e.getMessage(), e);
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    /** Maps a ResultSet row to a User domain object. */
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getString("UserId"));
        u.setName(rs.getString("Name"));
        u.setEmail(rs.getString("Email"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setRole(UserRole.fromString(rs.getString("Role")));
        u.setStatus(UserStatus.fromString(rs.getString("Status")));
        u.setFailedLoginAttempts(rs.getInt("FailedLoginAttempts"));
        Timestamp lastLogin = rs.getTimestamp("LastLoginDate");
        if (lastLogin != null) u.setLastLoginDate(lastLogin.toLocalDateTime());
        u.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
        return u;
    }
}