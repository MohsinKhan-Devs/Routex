package com.routex.dal;
import com.routex.model.InventoryItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for the InventoryItem table (UC02, UC03, UC04).
 *
 * FEFO (First-Expired-First-Out) ordering is applied when listing perishable
 * items — matching the business rule in UC02.
 */
public class InventoryItemDAO {

    private final Connection conn;

    public InventoryItemDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /**
     * Returns all active inventory items, joined with Warehouse name.
     * Items are ordered by ExpiryDate (FEFO) to assist perishable management.
     */
    public List<InventoryItem> findAll() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = """
            SELECT i.*, w.Name AS WarehouseName
            FROM InventoryItem i
            LEFT JOIN Warehouse w ON i.WarehouseId = w.WarehouseId
            WHERE i.IsActive = 1
            ORDER BY i.ExpiryDate ASC
            """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("InventoryItemDAO.findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Finds a single item by its UUID. */
    public InventoryItem findById(String itemId) {
        String sql = """
            SELECT i.*, w.Name AS WarehouseName
            FROM InventoryItem i
            LEFT JOIN Warehouse w ON i.WarehouseId = w.WarehouseId
            WHERE i.ItemId = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("InventoryItemDAO.findById failed: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Returns items whose Quantity is at or below their ReorderThreshold.
     * This list drives the automatic order generation logic in UC04.
     */
    public List<InventoryItem> findBelowThreshold() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = """
            SELECT i.*, w.Name AS WarehouseName
            FROM InventoryItem i
            LEFT JOIN Warehouse w ON i.WarehouseId = w.WarehouseId
            WHERE i.IsActive = 1 AND i.Quantity <= i.ReorderThreshold
            ORDER BY i.ExpiryDate ASC
            """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("InventoryItemDAO.findBelowThreshold failed: " + e.getMessage(), e);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    /** Inserts a new inventory item (UC02 — Add). */
    public boolean save(InventoryItem item) {
        String sql = """
            INSERT INTO InventoryItem
              (ItemId, SKU, Name, Quantity, ExpiryDate, WarehouseId, ReorderThreshold, IsActive)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, item.getSku());
            ps.setString(3, item.getName());
            ps.setInt(4, item.getQuantity());
            ps.setDate(5, Date.valueOf(item.getExpiryDate()));
            ps.setString(6, item.getWarehouseId());
            ps.setInt(7, item.getReorderThreshold());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("InventoryItemDAO.save failed: " + e.getMessage(), e);
        }
    }

    /** Updates an existing item's quantity, threshold, and other attributes (UC02 — Update). */
    public boolean update(InventoryItem item) {
        String sql = """
            UPDATE InventoryItem
            SET Name=?, Quantity=?, ExpiryDate=?, WarehouseId=?,
                ReorderThreshold=?, LastRestockDate=GETDATE()
            WHERE ItemId=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setInt(2, item.getQuantity());
            ps.setDate(3, Date.valueOf(item.getExpiryDate()));
            ps.setString(4, item.getWarehouseId());
            ps.setInt(5, item.getReorderThreshold());
            ps.setString(6, item.getItemId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("InventoryItemDAO.update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Soft-deletes an item by setting IsActive = 0 (UC02 — Remove).
     * Soft deletion preserves referential integrity for historical shipment orders.
     */
    public boolean softDelete(String itemId) {
        String sql = "UPDATE InventoryItem SET IsActive = 0 WHERE ItemId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("InventoryItemDAO.softDelete failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private InventoryItem mapRow(ResultSet rs) throws SQLException {
        InventoryItem item = new InventoryItem();
        item.setItemId(rs.getString("ItemId"));
        item.setSku(rs.getString("SKU"));
        item.setName(rs.getString("Name"));
        item.setQuantity(rs.getInt("Quantity"));
        item.setExpiryDate(rs.getDate("ExpiryDate").toLocalDate());
        item.setWarehouseId(rs.getString("WarehouseId"));
        item.setWarehouseName(rs.getString("WarehouseName"));
        item.setReorderThreshold(rs.getInt("ReorderThreshold"));
        item.setActive(rs.getBoolean("IsActive"));
        Timestamp restock = rs.getTimestamp("LastRestockDate");
        if (restock != null) item.setLastRestockDate(restock.toLocalDateTime());
        return item;
    }
}