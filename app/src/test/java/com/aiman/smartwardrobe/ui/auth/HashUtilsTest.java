package com.aiman.smartwardrobe.ui.auth;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ============================================================================
 * HashUtilsTest — Unit Tests for Password Hashing and Verification
 * ============================================================================
 *
 * <p>Tests the correctness of the upgraded PBKDF2-HMAC-SHA256 password hashing
 * mechanism and validates that legacy SHA-256 password hashes remain supported
 * for backward compatibility.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class HashUtilsTest {

    @Test
    public void testHashAndVerifyPassword() {
        String password = "mySecurePassword123";
        String hashedPassword = HashUtils.hashPassword(password);
        
        assertNotNull(hashedPassword);
        assertNotEquals("", hashedPassword);
        assertNotEquals(password, hashedPassword);
        
        // Verify matching password
        assertTrue(HashUtils.verifyPassword(password, hashedPassword));
        
        // Verify with wrong password
        assertFalse(HashUtils.verifyPassword("wrongPassword", hashedPassword));
    }

    @Test
    public void testLegacySha256Verification() {
        // A legacy SHA-256 hash of "password123" without salt (using old HashUtils behavior)
        // SHA-256("password123") = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f"
        String legacyHash = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
        
        assertTrue(HashUtils.verifyPassword("password123", legacyHash));
        assertFalse(HashUtils.verifyPassword("wrongPassword", legacyHash));
    }

    @Test
    public void testNullInputs() {
        assertFalse(HashUtils.verifyPassword(null, "someHash"));
        assertFalse(HashUtils.verifyPassword("password", null));
        assertFalse(HashUtils.verifyPassword(null, null));
        assertEquals("", HashUtils.hashPassword(null));
    }
}
