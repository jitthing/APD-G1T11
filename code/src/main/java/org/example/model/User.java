package org.example.model;

/**
 * Represents a user with username and hashed password.
 * Using Java Record for immutability and concise syntax.
 */
public record User(String username, String hashedPassword) {
    /**
     * Creates a new User with validation.
     *
     * @param username       the username
     * @param hashedPassword the SHA-256 hashed password
     * @throws IllegalArgumentException if either parameter is null or empty
     */
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be null or empty");
        }
    }
}
