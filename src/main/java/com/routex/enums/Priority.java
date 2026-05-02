package com.routex.enums;
/**
 * Priority level assigned to a ShipmentOrder (UC04).
 * CRITICAL orders (e.g., medical supplies) are dispatched before LOW priority orders.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static Priority fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}