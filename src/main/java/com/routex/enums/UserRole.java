package com.routex.enums;
/**
 * Defines the four roles that a user can be assigned in the RouteX system.
 * Role-based access control (RBAC) is enforced by checking this enum in
 * each UI controller and service method.
 */
public enum UserRole {
    SYSTEM_ADMIN,
    INVENTORY_MANAGER,
    FLEET_DISPATCHER,
    FIELD_DRIVER;

    /** Returns the enum constant matching the DB string value. */
    public static UserRole fromString(String value) {
        return valueOf(value.toUpperCase());
    }

    /** Human-readable label shown in the UI. */
    public String getDisplayName() {
        return switch (this) {
            case SYSTEM_ADMIN      -> "System Administrator";
            case INVENTORY_MANAGER -> "Inventory Manager";
            case FLEET_DISPATCHER  -> "Fleet Dispatcher";
            case FIELD_DRIVER      -> "Field Driver";
        };
    }
}