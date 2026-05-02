package com.routex.enums;
/**
 * Represents the operational state of a vehicle (UC06, UC10).
 * MAINTENANCE_HOLD vehicles are automatically excluded from assignment suggestions.
 */
public enum VehicleStatus {
    AVAILABLE,
    IN_TRANSIT,
    MAINTENANCE_HOLD;

    public static VehicleStatus fromString(String value) {
        return valueOf(value.toUpperCase());
    }

    public String getDisplayName() {
        return switch (this) {
            case AVAILABLE        -> "Available";
            case IN_TRANSIT       -> "In Transit";
            case MAINTENANCE_HOLD -> "Maintenance Hold";
        };
    }
}