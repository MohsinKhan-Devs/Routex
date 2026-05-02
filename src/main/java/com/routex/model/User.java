package com.routex.model;
import com.routex.enums.UserRole;
import com.routex.enums.UserStatus;
import java.time.LocalDateTime;

/**
 * Domain model representing a system user (UC01, UC11).
 *
 * OOP Principles:
 *  - Encapsulation: all fields are private; accessed via getters/setters
 *  - Abstraction: hides raw DB types behind meaningful domain types
 *
 * GRASP: Information Expert — the User object holds everything about a user account.
 */
public class User {

    private String   userId;
    private String   name;
    private String   email;
    private String   passwordHash;
    private UserRole role;
    private UserStatus status;
    private int      failedLoginAttempts;
    private LocalDateTime lastLoginDate;
    private LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Default constructor required for reflection/DAO usage. */
    public User() {}

    /** Full constructor used when creating a new user account (UC11). */
    public User(String userId, String name, String email, String passwordHash,
                UserRole role, UserStatus status) {
        this.userId       = userId;
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.status       = status;
        this.failedLoginAttempts = 0;
        this.createdAt    = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Business Logic Methods
    // -------------------------------------------------------------------------

    /** Returns true if the account is in a state that allows login. */
    public boolean isLoginAllowed() {
        return status == UserStatus.ACTIVE;
    }

    /** Increments the failed attempt counter; called by AuthService on bad password. */
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    /** Resets the counter on successful login. */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    /** Locks the account — called after 3 consecutive failures. */
    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getUserId()               { return userId; }
    public void   setUserId(String id)      { this.userId = id; }

    public String getName()                 { return name; }
    public void   setName(String name)      { this.name = name; }

    public String getEmail()                { return email; }
    public void   setEmail(String email)    { this.email = email; }

    public String getPasswordHash()             { return passwordHash; }
    public void   setPasswordHash(String hash)  { this.passwordHash = hash; }

    public UserRole getRole()               { return role; }
    public void     setRole(UserRole role)  { this.role = role; }

    public UserStatus getStatus()                   { return status; }
    public void       setStatus(UserStatus status)  { this.status = status; }

    public int  getFailedLoginAttempts()          { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int count) { this.failedLoginAttempts = count; }

    public LocalDateTime getLastLoginDate()                { return lastLoginDate; }
    public void          setLastLoginDate(LocalDateTime d) { this.lastLoginDate = d; }

    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void          setCreatedAt(LocalDateTime d) { this.createdAt = d; }

    @Override
    public String toString() {
        return name + " (" + role.getDisplayName() + ")";
    }
}