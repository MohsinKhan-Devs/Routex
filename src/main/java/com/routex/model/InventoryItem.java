package com.routex.model;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a stock-keeping unit tracked in the RouteX inventory (UC02, UC03).
 *
 * OOP Principles:
 *  - Encapsulation: private state with controlled access
 *
 * GRASP: Information Expert — this object knows its own threshold and
 *        whether it needs a reorder, so it exposes isBelowThreshold().
 */
public class InventoryItem {

    private String        itemId;
    private String        sku;
    private String        name;
    private int           quantity;
    private LocalDate     expiryDate;
    private String        warehouseId;
    private String        warehouseName;    // Denormalised for display convenience
    private int           reorderThreshold;
    private LocalDateTime lastRestockDate;
    private boolean       isActive;

    public InventoryItem() {}

    public InventoryItem(String itemId, String sku, String name, int quantity,
                         LocalDate expiryDate, String warehouseId,
                         int reorderThreshold, boolean isActive) {
        this.itemId           = itemId;
        this.sku              = sku;
        this.name             = name;
        this.quantity         = quantity;
        this.expiryDate       = expiryDate;
        this.warehouseId      = warehouseId;
        this.reorderThreshold = reorderThreshold;
        this.lastRestockDate  = LocalDateTime.now();
        this.isActive         = isActive;
    }

    // -------------------------------------------------------------------------
    // Business Logic (GRASP: Information Expert)
    // -------------------------------------------------------------------------

    /**
     * Returns true when the current stock is at or below the configured
     * reorder threshold — the trigger condition for UC04.
     */
    public boolean isBelowThreshold() {
        return quantity <= reorderThreshold;
    }

    /**
     * Determines whether FEFO (First-Expired-First-Out) should be applied.
     * Perishable items are those expiring within 730 days (~2 years).
     */
    public boolean isPerishable() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now().plusDays(730));
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getItemId()               { return itemId; }
    public void   setItemId(String itemId)  { this.itemId = itemId; }

    public String getSku()          { return sku; }
    public void   setSku(String sku){ this.sku = sku; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public int  getQuantity()           { return quantity; }
    public void setQuantity(int qty)    { this.quantity = qty; }

    public LocalDate getExpiryDate()               { return expiryDate; }
    public void      setExpiryDate(LocalDate date) { this.expiryDate = date; }

    public String getWarehouseId()                   { return warehouseId; }
    public void   setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseName()                     { return warehouseName; }
    public void   setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public int  getReorderThreshold()                { return reorderThreshold; }
    public void setReorderThreshold(int threshold)   { this.reorderThreshold = threshold; }

    public LocalDateTime getLastRestockDate()                  { return lastRestockDate; }
    public void          setLastRestockDate(LocalDateTime date){ this.lastRestockDate = date; }

    public boolean isActive()             { return isActive; }
    public void    setActive(boolean act) { this.isActive = act; }

    @Override
    public String toString() { return name + " [" + sku + "]"; }
}