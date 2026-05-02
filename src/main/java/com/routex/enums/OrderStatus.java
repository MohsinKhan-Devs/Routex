package com.routex.enums;
/**
 * Lifecycle states for a ShipmentOrder (UC04, UC05).
 * Transitions: PENDING_APPROVAL → APPROVED | REJECTED → DISPATCHED
 */
public enum OrderStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    DISPATCHED;

    public static OrderStatus fromString(String value) {
        return valueOf(value.toUpperCase());
    }

    public String getDisplayName() {
        return switch (this) {
            case PENDING_APPROVAL -> "Pending Approval";
            case APPROVED         -> "Approved";
            case REJECTED         -> "Rejected";
            case DISPATCHED       -> "Dispatched";
        };
    }
}