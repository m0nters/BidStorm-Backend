package com.taitrinh.online_auction.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility class for password generation and manipulation
 */
public class PasswordUtil {

    private static final Random RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generate a secure random password with 16 characters
     * Format: uppercase, lowercase, numbers, and special characters
     * Example: X7k#9pL2mNq$8vR4
     * 
     * @return A randomly generated secure password
     */
    public static String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "#$@!%*?&";
        String allChars = upperCase + lowerCase + numbers + specialChars;

        StringBuilder password = new StringBuilder(16);

        // Ensure at least one of each type
        password.append(upperCase.charAt(RANDOM.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(RANDOM.nextInt(lowerCase.length())));
        password.append(numbers.charAt(RANDOM.nextInt(numbers.length())));
        password.append(specialChars.charAt(RANDOM.nextInt(specialChars.length())));

        // Fill the rest randomly
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(RANDOM.nextInt(allChars.length())));
        }

        // Shuffle the password to avoid predictable pattern
        return shuffleString(password.toString());
    }

    /**
     * Shuffle a string randomly
     * 
     * @param input The string to shuffle
     * @return The shuffled string
     */
    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
