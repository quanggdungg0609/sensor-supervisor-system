package org.quangdung.core.utils.password_util;

import java.security.SecureRandom;
import org.mindrot.jbcrypt.BCrypt;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Singleton;

@Singleton
public class PasswordUtil implements IPasswordUtil {
    @ConfigProperty(name = "app.security.bcrypt.salt-rounds", defaultValue = "12")
    private int saltRounds;

    @ConfigProperty(name = "app.security.bcrypt.custom-salt")
    private String customSalt;

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%&*+-="; // Easy to type special characters
    private static final String ALL_CHARS = LOWERCASE + UPPERCASE + DIGITS + SPECIAL_CHARS;
    
    private static final SecureRandom random = new SecureRandom();

    /**
     * Hashes the input string using BCrypt algorithm with configured salt rounds.
     * 
     * @param input The plain text string to be hashed
     * @return The hashed string using BCrypt algorithm
     * @throws RuntimeException if hashing operation fails
     */
    @Override
    public String hash(String input) {
        try {
            if (input == null || input.trim().isEmpty()) {
                throw new IllegalArgumentException("Input cannot be null or empty");
            }
            
            // Use custom salt if provided, otherwise generate new salt with configured rounds
            String salt = (customSalt != null && !customSalt.trim().isEmpty()) 
                ? customSalt 
                : BCrypt.gensalt(saltRounds);
                
            return BCrypt.hashpw(input, salt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash input", e);
        }
    }

    /**
     * Verifies if the plain text matches the hashed text using BCrypt algorithm.
     * 
     * @param plainText The plain text string to verify
     * @param hashedText The hashed string to compare against
     * @return true if the plain text matches the hashed text, false otherwise
     * @throws RuntimeException if verification operation fails
     */
    @Override
    public boolean isMatch(String plainText, String hashedText) {
        try {
            if (plainText == null || hashedText == null) {
                return false;
            }
            
            if (plainText.trim().isEmpty() || hashedText.trim().isEmpty()) {
                return false;
            }
            
            return BCrypt.checkpw(plainText, hashedText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify password", e);
        }
    }
    
    /**
     * Validates if the input string is safe for hashing (not null, not empty, reasonable length).
     * 
     * @param input The input string to validate
     * @return true if the input is valid for hashing, false otherwise
     */
    public boolean isValidInput(String input) {
        return input != null && 
               !input.trim().isEmpty() && 
               input.length() <= 72; // BCrypt has a 72 character limit
    }
    
    /**
     * Gets the configured salt rounds for BCrypt hashing.
     * 
     * @return The number of salt rounds configured
     */
    public int getSaltRounds() {
        return saltRounds;
    }

    /**
     * Generates a random password with specified length that is easy to type.
     * The password will contain at least one special character and a mix of 
     * lowercase, uppercase letters and digits.
     * 
     * @param length The desired length of the password (minimum 4 characters)
     * @return A randomly generated password string
     * @throws IllegalArgumentException if length is less than 4
     */
    @Override
    public String generatePassword(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4 characters");
        }
        
        StringBuilder password = new StringBuilder(length);
        
        // Ensure at least one character from each category
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));
        
        // Fill the remaining length with random characters from all sets
        for (int i = 4; i < length; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }
        
        // Shuffle the password to avoid predictable patterns
        return shuffleString(password.toString());
    }
    
    /**
     * Shuffles the characters in a string to randomize their positions.
     * 
     * @param input The string to shuffle
     * @return A new string with characters in random order
     */
    private String shuffleString(String input) {
        char[] characters = input.toCharArray();
        
        // Fisher-Yates shuffle algorithm
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        
        return new String(characters);
    }
}
