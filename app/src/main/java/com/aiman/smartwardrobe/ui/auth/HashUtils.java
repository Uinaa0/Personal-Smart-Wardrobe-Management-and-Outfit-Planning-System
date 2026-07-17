package com.aiman.smartwardrobe.ui.auth;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * ============================================================================
 * HashUtils — Security Helper Class for Password Hashing
 * ============================================================================
 *
 * <p>Provides PBKDF2-HMAC-SHA256 password hashing with a random per-user
 * salt. This replaces the previous SHA-256 implementation which was
 * vulnerable to rainbow table and brute-force attacks due to the absence
 * of a salt and the use of a fast hash function.</p>
 *
 * <p><b>Why PBKDF2?</b>
 * PBKDF2 is a key-stretching algorithm that applies the hash function
 * many times (65,536 iterations here), making brute-force attacks
 * computationally expensive. The random salt ensures that two users
 * with the same password produce different hashes.</p>
 *
 * <p><b>Storage Format:</b> {@code iterations:base64(salt):base64(hash)}</p>
 *
 * @author Aiman — Final Year Project
 * @version 2.0
 */
public class HashUtils {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** Number of PBKDF2 iterations — higher = slower brute-force attacks. */
    private static final int ITERATIONS = 65536;

    /** Length of the derived key in bits. */
    private static final int KEY_LENGTH = 256;

    /** Length of the salt in bytes. */
    private static final int SALT_LENGTH = 16;

    /** PBKDF2 algorithm identifier available in JDK. */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Hashes a raw plaintext password using PBKDF2-HMAC-SHA256 with a
     * randomly generated salt.
     *
     * @param password The raw password to hash
     * @return A string in the format {@code iterations:salt:hash}, or
     *         empty string on error
     */
    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            // Generate a cryptographically secure random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Derive the hash
            byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH);

            // Encode and return as iterations:salt:hash
            return ITERATIONS + ":"
                    + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Verifies a raw plaintext password against a stored PBKDF2 hash.
     *
     * <p>The stored hash must be in the format produced by
     * {@link #hashPassword(String)}: {@code iterations:salt:hash}</p>
     *
     * @param password   The raw password to verify
     * @param storedHash The previously hashed password string
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        // Support legacy SHA-256 hashes (64 hex chars, no colons)
        // This allows existing accounts to still log in after the upgrade
        if (!storedHash.contains(":")) {
            return verifyLegacySha256(password, storedHash);
        }

        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) return false;

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            // Re-derive the hash with the same salt and iterations
            byte[] actualHash = pbkdf2(password, salt, iterations, KEY_LENGTH);

            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(expectedHash, actualHash);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    /**
     * Derives a PBKDF2 hash from the given password and salt.
     */
    private static byte[] pbkdf2(String password, byte[] salt,
                                  int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     * Always compares all bytes regardless of mismatches.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Backwards-compatible verification for legacy SHA-256 hashes
     * (stored before the PBKDF2 upgrade). Once the user changes their
     * password, it will be re-hashed with PBKDF2.
     */
    private static boolean verifyLegacySha256(String password, String storedHash) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(storedHash);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}
