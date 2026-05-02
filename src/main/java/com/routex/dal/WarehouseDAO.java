package com.routex.dal;
import com.routex.model.Warehouse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the Warehouse table.
 */
public class WarehouseDAO {

    private final Connection conn;

    public WarehouseDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /** Returns all active warehouses (used to populate combo boxes in UC02). */
    public List<Warehouse> findAll() {
        List<Warehouse> list = new ArrayList<>();
        String sql = "SELECT * FROM Warehouse WHERE IsActive = 1 ORDER BY Name";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("WarehouseDAO.findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Finds a warehouse by its UUID primary key. */
    public Warehouse findById(String warehouseId) {
        String sql = "SELECT * FROM Warehouse WHERE WarehouseId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("WarehouseDAO.findById failed: " + e.getMessage(), e);
        }
        return null;
    }

    private Warehouse mapRow(ResultSet rs) throws SQLException {
        return new Warehouse(
            rs.getString("WarehouseId"),
            rs.getString("Name"),
            rs.getString("LocationAddress"),
            rs.getInt("MaxCapacity"),
            rs.getBoolean("IsActive")
        );
    }
}