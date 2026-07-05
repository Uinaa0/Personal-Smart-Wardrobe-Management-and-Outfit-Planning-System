package com.aiman.smartwardrobe.ui.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ============================================================================
 * HashUtils — Security Helper Class for Password Hashing
 * ============================================================================
 *
 * <p>Provides SHA-256 password hashing functionality to ensure user passwords
 * are never stored or compared in plaintext in SharedPreferences.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class HashUtils {

    /**
     * Hashes a raw plaintext password string using the SHA-256 algorithm.
     * Returns a hexadecimal representation of the hash.
     *
     * @param password The raw password to hash
     * @return The SHA-256 hashed password as a Hex string, or empty string on error
     */
    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}
