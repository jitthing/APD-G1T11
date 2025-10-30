package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Utility class for SHA-256 hashing operations.
 * Uses ThreadLocal MessageDigest for thread-safe, high-performance hashing.
 * Optimized with Java 25 Vector API (JEP 508) for SIMD-accelerated hex conversion.
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

    /**
     * Vector species for SIMD operations - uses the preferred vector size for the hardware.
     * Typically 128-bit (SSE), 256-bit (AVX2), or 512-bit (AVX-512).
     */
    private static final VectorSpecies<Byte> VECTOR_SPECIES = ByteVector.SPECIES_PREFERRED;

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
     * Converts byte array to hexadecimal string using Vector API (JEP 508).
     * Utilizes SIMD instructions for parallel processing of multiple bytes.
     * On AVX2 hardware, processes 16-32 bytes per iteration vs 1 byte in scalar code.
     *
     * @param bytes the byte array (typically 32 bytes for SHA-256)
     * @return hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = HEX_CHARS_BUFFER.get();
        int vectorLength = VECTOR_SPECIES.length();
        int i = 0;

        // Vectorized loop - process multiple bytes in parallel using SIMD
        for (; i < VECTOR_SPECIES.loopBound(bytes.length); i += vectorLength) {
            // Load vector of bytes from the hash array
            ByteVector vector = ByteVector.fromArray(VECTOR_SPECIES, bytes, i);

            // Extract high nibbles (upper 4 bits) in parallel
            ByteVector highNibbles = vector.lanewise(VectorOperators.ASHR, 4)
                                          .lanewise(VectorOperators.AND, 0x0F);

            // Extract low nibbles (lower 4 bits) in parallel
            ByteVector lowNibbles = vector.lanewise(VectorOperators.AND, 0x0F);

            // Convert nibbles to hex chars and store in output buffer
            byte[] highBytes = highNibbles.toArray();
            byte[] lowBytes = lowNibbles.toArray();

            for (int j = 0; j < vectorLength && (i + j) < bytes.length; j++) {
                hexChars[(i + j) * 2] = HEX_ARRAY[highBytes[j]];
                hexChars[(i + j) * 2 + 1] = HEX_ARRAY[lowBytes[j]];
            }
        }

        // Handle remaining bytes (if any) with scalar code
        // For SHA-256 (32 bytes), this is typically not needed with 128/256/512-bit vectors
        for (; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, 0, bytes.length * 2);
    }
}
