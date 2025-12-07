package com.taitrinh.online_auction.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility class for generating SEO-friendly URL slugs.
 * Handles Vietnamese character conversion and URL-safe formatting.
 */
public class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");

    /**
     * Generate a URL-friendly slug from the given text.
     * 
     * Process:
     * 1. Convert Vietnamese characters to ASCII (ă → a, ư → u, etc.)
     * 2. Convert to lowercase
     * 3. Replace spaces with hyphens
     * 4. Remove special characters
     * 5. Remove consecutive hyphens
     * 6. Remove leading/trailing hyphens
     * 
     * @param input Text to convert to slug
     * @return URL-friendly slug
     */
    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // Normalize Vietnamese characters to ASCII
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // Replace spaces with hyphens
        String noWhitespace = WHITESPACE.matcher(normalized).replaceAll("-");

        // Remove accents/diacritics
        String noAccents = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(noWhitespace).replaceAll("");

        // Convert to lowercase
        String lowercase = noAccents.toLowerCase();

        // Remove non-word characters (except hyphens)
        String slug = NONLATIN.matcher(lowercase).replaceAll("");

        // Replace multiple consecutive hyphens with single hyphen
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");

        // Remove hyphens at the start and end
        slug = EDGESDHASHES.matcher(slug).replaceAll("");

        return slug;
    }

    /**
     * Generate a unique slug by appending a number if the slug already exists.
     * 
     * @param baseSlug      The base slug to make unique
     * @param existsChecker Function that checks if a slug already exists
     * @return Unique slug (may have -2, -3, etc. appended)
     */
    public static String makeUnique(String baseSlug, java.util.function.Predicate<String> existsChecker) {
        String slug = baseSlug;
        int counter = 2;

        while (existsChecker.test(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    /**
     * Generate hierarchical slug for child category.
     * Format: parent-slug/child-slug
     * 
     * @param parentSlug Parent category slug
     * @param childName  Child category name
     * @return Hierarchical slug
     */
    public static String toHierarchicalSlug(String parentSlug, String childName) {
        String childSlug = toSlug(childName);

        if (parentSlug == null || parentSlug.trim().isEmpty()) {
            return childSlug;
        }

        return parentSlug + "/" + childSlug;
    }
}
