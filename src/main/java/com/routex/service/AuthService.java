package com.routex.service;
import com.routex.enums.UserStatus;
import com.routex.model.User;
import com.routex.dal.AuditLogDAO;
import com.routex.dal.UserDAO;
import com.routex.util.PasswordUtil;

/**
 * Business logic service for UC01 — Login / Authenticate.
 *
 * Responsibilities:
 *  1. Validate credentials against the stored SHA-256 hash.
 *  2. Enforce account lockout after 3 consecutive failures.
 *  3. Write an audit log entry for every login attempt.
 *
 * Design Patterns:
 *  - GRASP Controller: AuthService acts as the system controller for the
 *    login use case, delegating persistence to UserDAO.
 *  - Service Layer (GoF): encapsulates business rules that span the DAL.
 *
 * OOP Principles:
 *  - Single Responsibility: this class only handles authentication logic.
 *  - Low Coupling: it depends only on DAO interfaces, not on UI components.
 */
public class AuthService {

    /** Maximum consecutive failures before the account is locked. */
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserDAO     userDAO;
    private final AuditLogDAO auditDAO;

    public AuthService() {
        this.userDAO  = new UserDAO();
        this.auditDAO = new AuditLogDAO();
    }

    // -------------------------------------------------------------------------
    // UC01 — Login
    // -------------------------------------------------------------------------

    /**
     * Attempts to authenticate a user with the supplied credentials.
     *
     * @param email     the user's registered email address
     * @param password  the plain-text password entered by the user
     * @return the authenticated User on success
     * @throws AuthException with a descriptive message on failure
     */
    public User login(String email, String password) throws AuthException {

        // 1. Look up the account
        User user = userDAO.findByEmail(email);
        if (user == null) {
            throw new AuthException("No account found for email: " + email);
        }

        // 2. Check account status before verifying the password
        if (user.getStatus() == UserStatus.LOCKED) {
            auditDAO.log(user.getUserId(), "LOGIN_FAILED_LOCKED", "Users", null);
            throw new AuthException("Account is locked due to too many failed attempts. "
                                  + "Contact your System Administrator.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AuthException("Account is inactive. Contact your System Administrator.");
        }

        // 3. Verify the password hash
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            handleFailedAttempt(user);
            int remaining = MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
            if (remaining <= 0) {
                throw new AuthException("Account locked — too many failed attempts.");
            }
            throw new AuthException("Incorrect password. " + remaining + " attempt(s) remaining.");
        }

        // 4. Successful login — reset counter and record timestamp
        userDAO.recordSuccessfulLogin(user.getUserId());
        user.resetFailedAttempts();
        auditDAO.log(user.getUserId(), "LOGIN_SUCCESS", "Users", null);

        return user;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Increments the failed attempt counter.
     * If the threshold is reached, the account is locked.
     */
    private void handleFailedAttempt(User user) {
        user.incrementFailedAttempts();
        UserStatus newStatus = user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS
                             ? UserStatus.LOCKED
                             : UserStatus.ACTIVE;
        userDAO.updateLoginAttempts(user.getUserId(), user.getFailedLoginAttempts(), newStatus);
        auditDAO.log(user.getUserId(), "LOGIN_FAILED", "Users", null);
    }

    // -------------------------------------------------------------------------
    // Inner Exception Class
    // -------------------------------------------------------------------------

    /**
     * Thrown when authentication fails for any reason.
     * The message is safe to display in the UI.
     */
    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }
}