package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for SHA-256 hashing operations.
 * Uses ThreadLocal MessageDigest for thread-safe, high-performance hashing.
 */
public final class HashUtil {

    /**
     * ThreadLocal MessageDigest to avoid synchronization overhead in concurrent environment.
     * Each thread gets its own MessageDigest instance.
     */
    private static final ThreadLocal<MessageDigest> MESSAGE_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    });

    /**
     * ThreadLocal char array for hex conversion - reused to reduce allocations.
     * SHA-256 produces 32 bytes = 64 hex chars.
     */
    private static final ThreadLocal<char[]> HEX_CHARS_BUFFER = ThreadLocal.withInitial(() -> new char[64]);

    /**
     * Lookup table for fast byte to hex conversion.
     * Pre-computed to avoid runtime calculations.
     */
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    // Private constructor to prevent instantiation
    private HashUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Computes SHA-256 hash of the input string.
     * Thread-safe and optimized for concurrent use.
     *
     * @param input the string to hash
     * @return the SHA-256 hash as hexadecimal string
     */
    public static String sha256(String input) {
        var digest = MESSAGE_DIGEST.get();
        digest.reset(); // Reset for reuse
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }


    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /**
     * Converts byte array to hexadecimal string.
     * Highly optimized using lookup table and ThreadLocal buffer for maximum performance.
     *
     * @param bytes the byte array
     * @return hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        // Reuse ThreadLocal buffer for SHA-256 (32 bytes = 64 chars)
        char[] hexChars = HEX_CHARS_BUFFER.get();

        // Use lookup table for faster conversion - no conditionals
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];  // High nibble
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];  // Low nibble
        }

        // Create string from exact length needed
        return new String(hexChars, 0, bytes.length * 2);
    }
}
