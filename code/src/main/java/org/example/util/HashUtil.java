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

    /**
     * Converts byte array to hexadecimal string.
     * Optimized implementation for performance.
     *
     * @param bytes the byte array
     * @return hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        var hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
