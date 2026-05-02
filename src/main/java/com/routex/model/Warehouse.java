package com.routex.model;
/**
 * Represents a physical warehouse location where inventory is stored.
 * Introduced for proper normalisation (UC02 — items reference a warehouse).
 *
 * OOP Principles:
 *  - Encapsulation: state is private; mutated only through setters
 */
public class Warehouse {

    private String  warehouseId;
    private String  name;
    private String  locationAddress;
    private int     maxCapacity;
    private boolean isActive;

    public Warehouse() {}

    public Warehouse(String warehouseId, String name, String locationAddress,
                     int maxCapacity, boolean isActive) {
        this.warehouseId     = warehouseId;
        this.name            = name;
        this.locationAddress = locationAddress;
        this.maxCapacity     = maxCapacity;
        this.isActive        = isActive;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getWarehouseId()                  { return warehouseId; }
    public void   setWarehouseId(String warehouseId){ this.warehouseId = warehouseId; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getLocationAddress()                      { return locationAddress; }
    public void   setLocationAddress(String locationAddress){ this.locationAddress = locationAddress; }

    public int  getMaxCapacity()              { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity){ this.maxCapacity = maxCapacity; }

    public boolean isActive()              { return isActive; }
    public void    setActive(boolean active){ this.isActive = active; }

    @Override
    public String toString() { return name; }
}