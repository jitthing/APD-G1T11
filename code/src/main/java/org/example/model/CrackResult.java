package org.example.model;

/**
 * Represents a successfully cracked password result.
 * Contains username, the original hash, and the discovered plain text password.
 */
public record CrackResult(String username, String hashedPassword, String plainPassword) {
    /**
     * Creates a new CrackResult with validation.
     *
     * @param username       the username
     * @param hashedPassword the SHA-256 hash
     * @param plainPassword  the discovered plain text password
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public CrackResult {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be null or empty");
        }
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Plain password cannot be null or empty");
        }
    }

    /**
     * Formats the crack result as a CSV line.
     *
     * @return CSV formatted string: username,hashedPassword,plainPassword
     */
    public String toCsvLine() {
        return String.format("%s,%s,%s", username, hashedPassword, plainPassword);
    }
}
