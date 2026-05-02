package com.routex.dal;
import com.routex.enums.OrderStatus;
import com.routex.enums.Priority;
import com.routex.model.ShipmentOrder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for the ShipmentOrder table (UC04, UC05).
 */
public class ShipmentOrderDAO {

    private final Connection conn;

    public ShipmentOrderDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** Returns all orders, joined with the item name for display. */
    public List<ShipmentOrder> findAll() {
        List<ShipmentOrder> list = new ArrayList<>();
        String sql = """
            SELECT o.*, i.Name AS ItemName
            FROM ShipmentOrder o
            LEFT JOIN InventoryItem i ON o.ItemId = i.ItemId
            ORDER BY o.CreatedAt DESC
            """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentOrderDAO.findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Returns orders filtered by their status (e.g., PENDING_APPROVAL for UC05). */
    public List<ShipmentOrder> findByStatus(OrderStatus status) {
        List<ShipmentOrder> list = new ArrayList<>();
        String sql = """
            SELECT o.*, i.Name AS ItemName
            FROM ShipmentOrder o
            LEFT JOIN InventoryItem i ON o.ItemId = i.ItemId
            WHERE o.Status = ?
            ORDER BY o.CreatedAt DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentOrderDAO.findByStatus failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Finds a single order by its UUID. */
    public ShipmentOrder findById(String orderId) {
        String sql = """
            SELECT o.*, i.Name AS ItemName
            FROM ShipmentOrder o
            LEFT JOIN InventoryItem i ON o.ItemId = i.ItemId
            WHERE o.OrderId = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentOrderDAO.findById failed: " + e.getMessage(), e);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    /**
     * Inserts a new draft shipment order (UC04).
     * Status is always PENDING_APPROVAL at creation.
     */
    public boolean save(ShipmentOrder order) {
        String sql = """
            INSERT INTO ShipmentOrder
              (OrderId, ItemId, RequiredQty, DestinationAddress, Priority,
               Status, ExpectedDeliveryDate)
            VALUES (?, ?, ?, ?, ?, 'PENDING_APPROVAL', ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, order.getItemId());
            ps.setInt(3, order.getRequiredQty());
            ps.setString(4, order.getDestinationAddress());
            ps.setString(5, order.getPriority().name());
            if (order.getExpectedDeliveryDate() != null)
                ps.setDate(6, Date.valueOf(order.getExpectedDeliveryDate()));
            else
                ps.setNull(6, Types.DATE);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentOrderDAO.save failed: " + e.getMessage(), e);
        }
    }

    /**
     * Approves a pending order — transitions status to APPROVED (UC05).
     * Also allows quantity adjustment at approval time.
     */
    public boolean approve(String orderId, int adjustedQty) {
        String sql = """
            UPDATE ShipmentOrder
            SET Status='APPROVED', RequiredQty=?
            WHERE OrderId=? AND Status='PENDING_APPROVAL'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adjustedQty);
            ps.setString(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentOrderDAO.approve failed: " + e.getMessage(), e);
        }
    }

    /**
     * Rejects a pending order, storing the mandatory rejection reason (UC05).
     */
    public boolean reject(String orderId, String reason) {
        String sql = """
            UPDATE ShipmentOrder
            SET Status='REJECTED', RejectionReason=?
            WHERE OrderId=? AND Status='PENDING_APPROVAL'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentOrderDAO.reject failed: " + e.getMessage(), e);
        }
    }

    /**
     * Marks an approved order as DISPATCHED once a shipment is created (UC06).
     */
    public boolean markDispatched(String orderId) {
        String sql = "UPDATE ShipmentOrder SET Status='DISPATCHED' WHERE OrderId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("ShipmentOrderDAO.markDispatched failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private ShipmentOrder mapRow(ResultSet rs) throws SQLException {
        ShipmentOrder o = new ShipmentOrder();
        o.setOrderId(rs.getString("OrderId"));
        o.setItemId(rs.getString("ItemId"));
        o.setItemName(rs.getString("ItemName"));
        o.setRequiredQty(rs.getInt("RequiredQty"));
        o.setDestinationAddress(rs.getString("DestinationAddress"));
        o.setPriority(Priority.fromString(rs.getString("Priority")));
        o.setStatus(OrderStatus.fromString(rs.getString("Status")));
        Date edd = rs.getDate("ExpectedDeliveryDate");
        if (edd != null) o.setExpectedDeliveryDate(edd.toLocalDate());
        o.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
        o.setRejectionReason(rs.getString("RejectionReason"));
        return o;
    }
}