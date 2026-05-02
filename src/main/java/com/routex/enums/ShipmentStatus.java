package com.routex.enums;
/**
 * Five-state lifecycle for a Shipment, advanced by the Field Driver (UC09).
 * DISPATCHED → IN_TRANSIT → DELIVERED (or CANCELLED at any point)
 */
public enum ShipmentStatus {
    DISPATCHED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    public static ShipmentStatus fromString(String value) {
        return valueOf(value.toUpperCase());
    }

    public String getDisplayName() {
        return switch (this) {
            case DISPATCHED -> "Dispatched";
            case IN_TRANSIT -> "In Transit";
            case DELIVERED  -> "Delivered";
            case CANCELLED  -> "Cancelled";
        };
    }
}