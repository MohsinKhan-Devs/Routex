package com.routex.util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for password hashing using SHA-256.
 *
 * Design Pattern: Utility / Static Helper
 *   - No instantiation needed; all methods are static.
 *
 * In a production system this would use BCrypt with a salt.
 * SHA-256 is used here to avoid external dependencies.
 */
public class PasswordUtil {

    private PasswordUtil() { /* prevent instantiation */ }

    /**
     * Returns the SHA-256 hex digest of the given plain-text password.
     *
     * @param plainText the raw password entered by the user
     * @return 64-character lowercase hex string
     */
    public static String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available.", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored SHA-256 hash.
     *
     * @param plainText    the raw password to verify
     * @param storedHash   the previously hashed value from the database
     * @return true if the hashes match
     */
    public static boolean verify(String plainText, String storedHash) {
        return hash(plainText).equalsIgnoreCase(storedHash);
    }
}