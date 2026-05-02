package com.routex.enums;
/**
 * Represents the lifecycle states of a user account.
 * LOCKED is applied automatically after three consecutive failed login attempts.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED;

    public static UserStatus fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}