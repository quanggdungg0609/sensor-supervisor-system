package org.quangdung.core.utils.password_util;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PasswordUtilTest {

    @Inject
    IPasswordUtil passwordUtil;

    // --- Test hash() and isMatch() methods ---

    @Test
    @DisplayName("Should hash password and successfully verify it")
    void testHashAndIsMatch_Success() {
        String plainPassword = "mySecretPassword123!";
        String hashedPassword = passwordUtil.hash(plainPassword);

        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(passwordUtil.isMatch(plainPassword, hashedPassword), "Password should match the hash");
    }

    @Test
    @DisplayName("Should return false for non-matching password")
    void testIsMatch_Failure() {
        String plainPassword = "mySecretPassword123!";
        String wrongPassword = "wrongPassword";
        String hashedPassword = passwordUtil.hash(plainPassword);

        assertFalse(passwordUtil.isMatch(wrongPassword, hashedPassword), "Wrong password should not match the hash");
    }

    @Test
    @DisplayName("hash() should throw IllegalArgumentException for null or empty input")
    void testHash_InvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hash(null), "Hashing null should throw exception");
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hash(""), "Hashing empty string should throw exception");
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hash("   "), "Hashing blank string should throw exception");
    }

    @Test
    @DisplayName("isMatch() should return false for null or empty inputs")
    void testIsMatch_InvalidInput() {
        String hashedPassword = passwordUtil.hash("aPassword");

        assertFalse(passwordUtil.isMatch(null, hashedPassword));
        assertFalse(passwordUtil.isMatch("aPassword", null));
        assertFalse(passwordUtil.isMatch("", hashedPassword));
        assertFalse(passwordUtil.isMatch("aPassword", ""));
        assertFalse(passwordUtil.isMatch("  ", hashedPassword));
        assertFalse(passwordUtil.isMatch("aPassword", "  "));
    }


    // --- Test generatePassword() method ---
    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32})
    @DisplayName("generatePassword() should create password of specified length")
    void testGeneratePassword_Length(int length) {
        String password = passwordUtil.generatePassword(length);
        assertNotNull(password);
        assertEquals(length, password.length(), "Generated password should have the specified length");
    }

    @Test
    @DisplayName("generatePassword() should contain all required character types")
    void testGeneratePassword_ContainsAllCharTypes() {
        String password = passwordUtil.generatePassword(20);
        
        // Regular expressions to check for character types
        assertTrue(password.matches(".*[a-z].*"), "Should contain at least one lowercase letter");
        assertTrue(password.matches(".*[A-Z].*"), "Should contain at least one uppercase letter");
        assertTrue(password.matches(".*[0-9].*"), "Should contain at least one digit");
        assertTrue(password.matches(".*[!@#$%&*+\\-=].*"), "Should contain at least one special character");
    }

    @Test
    @DisplayName("generatePassword() should throw IllegalArgumentException for length less than 4")
    void testGeneratePassword_InvalidLength() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.generatePassword(3);
        });
        assertEquals("Password length must be at least 4 characters", exception.getMessage());
    }

    // --- Test Configuration Properties ---
    @Test
    @DisplayName("Should use default salt rounds when not configured")
    void testGetSaltRounds_Default() {
        // The default value in your class is 12
        assertEquals(12, ((PasswordUtil) passwordUtil).getSaltRounds(), "Default salt rounds should be 12");
    }
}