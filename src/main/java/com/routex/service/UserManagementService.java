package com.routex.service;
import com.routex.enums.UserRole;
import com.routex.enums.UserStatus;
import com.routex.model.User;
import com.routex.dal.AuditLogDAO;
import com.routex.dal.UserDAO;
import com.routex.util.PasswordUtil;

import java.util.List;

/**
 * Business logic service for UC11 — Manage User Accounts.
 *
 * Business Rules:
 *  - Each account must have exactly one role.
 *  - At least one active SYSTEM_ADMIN account must exist at all times.
 *  - Deleting the last active admin is not permitted.
 *  - All account changes are recorded in the AuditLog.
 *
 * Design Patterns:
 *  - GRASP Controller: single controller for the user management use case.
 *  - Information Expert: UserDAO owns the data; this service owns the rules.
 */
public class UserManagementService {

    private final UserDAO     userDAO;
    private final AuditLogDAO auditDAO;

    public UserManagementService() {
        this.userDAO  = new UserDAO();
        this.auditDAO = new AuditLogDAO();
    }

    // -------------------------------------------------------------------------
    // UC11 — View
    // -------------------------------------------------------------------------

    /** Returns all user accounts for the System Admin's management screen. */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    // -------------------------------------------------------------------------
    // UC11 — Create
    // -------------------------------------------------------------------------

    /**
     * Creates a new user account with the specified role.
     *
     * @param name      display name
     * @param email     unique login email
     * @param password  plain-text password (hashed before storage)
     * @param role      the single role to assign
     * @param actorId   the System Admin's UserId
     */
    public void createUser(String name, String email, String password,
                           UserRole role, String actorId) throws UserManagementException {
        validateInput(name, email, password);

        // Check for duplicate email
        if (userDAO.findByEmail(email) != null)
            throw new UserManagementException("An account with email '" + email + "' already exists.");

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        boolean ok = userDAO.save(user);
        if (!ok) throw new UserManagementException("Failed to create user account.");
        auditDAO.log(actorId, "USER_ACCOUNT_CREATED", "Users", null);
    }

    // -------------------------------------------------------------------------
    // UC11 — Edit
    // -------------------------------------------------------------------------

    /**
     * Updates a user's name, email, role, and status.
     * Prevents deactivating or changing role of the last active admin.
     *
     * @param actorId the System Admin's UserId
     */
    public void updateUser(User updated, String actorId) throws UserManagementException {
        if (updated.getName() == null || updated.getName().isBlank())
            throw new UserManagementException("Name cannot be empty.");
        if (updated.getEmail() == null || updated.getEmail().isBlank())
            throw new UserManagementException("Email cannot be empty.");

        // Safety check: will this update leave zero active admins?
        User existing = userDAO.findById(updated.getUserId());
        if (existing != null
            && existing.getRole() == UserRole.SYSTEM_ADMIN
            && (updated.getRole() != UserRole.SYSTEM_ADMIN
                || updated.getStatus() != UserStatus.ACTIVE)) {
            if (userDAO.countActiveAdmins() <= 1) {
                throw new UserManagementException(
                    "Cannot change the role or deactivate the last active System Administrator.");
            }
        }

        boolean ok = userDAO.update(updated);
        if (!ok) throw new UserManagementException("Update failed — user not found.");
        auditDAO.log(actorId, "USER_ACCOUNT_UPDATED", "Users", null);
    }

    // -------------------------------------------------------------------------
    // UC11 — Deactivate
    // -------------------------------------------------------------------------

    /**
     * Sets a user account to INACTIVE (soft-disable).
     *
     * @param userId  UUID of the account to deactivate
     * @param actorId the System Admin's UserId
     */
    public void deactivateUser(String userId, String actorId) throws UserManagementException {
        User user = userDAO.findById(userId);
        if (user == null) throw new UserManagementException("User not found.");

        // Protect the last active admin
        if (user.getRole() == UserRole.SYSTEM_ADMIN && userDAO.countActiveAdmins() <= 1) {
            throw new UserManagementException(
                "Cannot deactivate the last active System Administrator.");
        }

        user.setStatus(UserStatus.INACTIVE);
        userDAO.update(user);
        auditDAO.log(actorId, "USER_ACCOUNT_DEACTIVATED", "Users", null);
    }

    // -------------------------------------------------------------------------
    // UC11 — Delete
    // -------------------------------------------------------------------------

    /**
     * Permanently deletes a user account.
     * Blocked if the account is the only active System Administrator.
     *
     * @param userId  UUID of the account to delete
     * @param actorId the System Admin's UserId
     */
    public void deleteUser(String userId, String actorId) throws UserManagementException {
        User user = userDAO.findById(userId);
        if (user == null) throw new UserManagementException("User not found.");

        if (user.getRole() == UserRole.SYSTEM_ADMIN
            && user.getStatus() == UserStatus.ACTIVE
            && userDAO.countActiveAdmins() <= 1) {
            throw new UserManagementException(
                "Cannot delete the last active System Administrator. "
                + "Assign this role to another user first.");
        }

        boolean ok = userDAO.delete(userId);
        if (!ok) throw new UserManagementException("Delete failed.");
        auditDAO.log(actorId, "USER_ACCOUNT_DELETED", "Users", null);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateInput(String name, String email, String password)
            throws UserManagementException {
        if (name == null || name.isBlank())
            throw new UserManagementException("Name is required.");
        if (email == null || email.isBlank() || !email.contains("@"))
            throw new UserManagementException("A valid email address is required.");
        if (password == null || password.length() < 6)
            throw new UserManagementException("Password must be at least 6 characters.");
    }

    // -------------------------------------------------------------------------
    // Inner Exception
    // -------------------------------------------------------------------------

    public static class UserManagementException extends Exception {
        public UserManagementException(String message) { super(message); }
    }
}