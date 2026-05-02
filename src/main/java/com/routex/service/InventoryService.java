package com.routex.service;
import com.routex.model.InventoryItem;
import com.routex.model.Warehouse;
import com.routex.dal.AuditLogDAO;
import com.routex.dal.InventoryItemDAO;
import com.routex.dal.WarehouseDAO;

import java.util.List;

/**
 * Business logic service for:
 *  - UC02 — Manage Inventory Stock (view, add, update, remove items)
 *  - UC03 — Set Reorder Thresholds (implicitly part of add/update item)
 *
 * Business Rules Enforced:
 *  - Quantity must be ≥ 0.
 *  - Reorder threshold must be > 0.
 *  - SKU must be unique (enforced by DB UNIQUE constraint; caught here).
 *  - FEFO ordering is applied by the DAO query.
 *
 * Design Patterns:
 *  - GRASP Controller: acts as the entry point for all inventory use cases.
 *  - Service Layer: isolates business rules from the UI layer.
 */
public class InventoryService {

    private final InventoryItemDAO inventoryDAO;
    private final WarehouseDAO     warehouseDAO;
    private final AuditLogDAO      auditDAO;

    public InventoryService() {
        this.inventoryDAO = new InventoryItemDAO();
        this.warehouseDAO = new WarehouseDAO();
        this.auditDAO     = new AuditLogDAO();
    }

    // -------------------------------------------------------------------------
    // UC02 — View
    // -------------------------------------------------------------------------

    /** Returns all active inventory items ordered by expiry date (FEFO). */
    public List<InventoryItem> getAllItems() {
        return inventoryDAO.findAll();
    }

    /** Returns all warehouses for combo box population. */
    public List<Warehouse> getAllWarehouses() {
        return warehouseDAO.findAll();
    }

    // -------------------------------------------------------------------------
    // UC02 — Add
    // -------------------------------------------------------------------------

    /**
     * Adds a new inventory item after validating business rules.
     *
     * @param actorId the UserId of the Inventory Manager performing the action
     */
    public void addItem(InventoryItem item, String actorId) throws InventoryException {
        validateItem(item, true);
        boolean saved = inventoryDAO.save(item);
        if (!saved) throw new InventoryException("Failed to save inventory item.");
        auditDAO.log(actorId, "INVENTORY_ITEM_ADDED", "InventoryItem", null);
    }

    // -------------------------------------------------------------------------
    // UC02 — Update / UC03 — Set Reorder Threshold
    // -------------------------------------------------------------------------

    /**
     * Updates an existing item's attributes including its reorder threshold (UC03).
     *
     * @param actorId the UserId of the Inventory Manager
     */
    public void updateItem(InventoryItem item, String actorId) throws InventoryException {
        if (item.getItemId() == null || item.getItemId().isBlank()) {
            throw new InventoryException("Item ID is required for update.");
        }
        validateItem(item, false);
        boolean updated = inventoryDAO.update(item);
        if (!updated) throw new InventoryException("Item not found or update failed.");
        auditDAO.log(actorId, "INVENTORY_ITEM_UPDATED", "InventoryItem", null);
    }

    // -------------------------------------------------------------------------
    // UC02 — Remove
    // -------------------------------------------------------------------------

    /**
     * Soft-deletes an inventory item (sets IsActive = false).
     * Hard deletion is avoided to preserve referential integrity in order history.
     *
     * @param actorId the UserId of the Inventory Manager
     */
    public void removeItem(String itemId, String actorId) throws InventoryException {
        if (itemId == null || itemId.isBlank()) {
            throw new InventoryException("Item ID is required for removal.");
        }
        boolean deleted = inventoryDAO.softDelete(itemId);
        if (!deleted) throw new InventoryException("Item not found.");
        auditDAO.log(actorId, "INVENTORY_ITEM_REMOVED", "InventoryItem", null);
    }

    // -------------------------------------------------------------------------
    // UC03 — Threshold Check (called by ShipmentOrderService)
    // -------------------------------------------------------------------------

    /**
     * Returns all items currently at or below their reorder threshold.
     * Used to trigger automatic shipment order generation (UC04).
     */
    public List<InventoryItem> getItemsBelowThreshold() {
        return inventoryDAO.findBelowThreshold();
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateItem(InventoryItem item, boolean requireSku) throws InventoryException {
        if (item.getName() == null || item.getName().isBlank())
            throw new InventoryException("Item name cannot be empty.");
        if (requireSku && (item.getSku() == null || item.getSku().isBlank()))
            throw new InventoryException("SKU cannot be empty.");
        if (item.getQuantity() < 0)
            throw new InventoryException("Quantity cannot be negative.");
        if (item.getReorderThreshold() <= 0)
            throw new InventoryException("Reorder threshold must be greater than zero.");
        if (item.getExpiryDate() == null)
            throw new InventoryException("Expiry date is required.");
        if (item.getWarehouseId() == null || item.getWarehouseId().isBlank())
            throw new InventoryException("A warehouse must be selected.");
    }

    // -------------------------------------------------------------------------
    // Inner Exception Class
    // -------------------------------------------------------------------------

    public static class InventoryException extends Exception {
        public InventoryException(String message) { super(message); }
    }
}