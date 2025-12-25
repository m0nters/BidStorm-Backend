package com.taitrinh.online_auction.util;

/**
 * Utility class for masking user names for privacy
 */
public class NameMaskingUtil {

    private NameMaskingUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Mask a name by showing only the last word (typically the given name in
     * Vietnamese)
     * This provides better privacy by hiding family name while maintaining
     * recognizability
     * 
     * Examples:
     * - "Nguyen Van Khoa" -> "****Khoa"
     * - "Tran Thi Mai" -> "****Mai"
     * - "Le A" -> "****A"
     * - "John" -> "****John"
     *
     * @param fullName The full name to mask
     * @return Masked name with last word visible, or null if input is null
     */
    public static String maskName(String fullName) {
        if (fullName == null) {
            return null;
        }

        String trimmedName = fullName.trim();
        if (trimmedName.isEmpty()) {
            return "****";
        }

        // Find the last word (after the last space)
        int lastSpaceIndex = trimmedName.lastIndexOf(' ');

        if (lastSpaceIndex == -1) {
            // Single word name - show entire name after mask
            return "****" + trimmedName;
        }

        // Extract last word (given name)
        String lastName = trimmedName.substring(lastSpaceIndex + 1);
        return "****" + lastName;
    }
}
