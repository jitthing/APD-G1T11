package org.example.service;

import org.example.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for loading users and their hashed passwords from input file.
 * Follows Single Responsibility Principle - only handles user data loading.
 */
public class HashLoaderService {

    /**
     * Loads users from the specified file.
     * Optimized with BufferedReader and pre-sized HashMap.
     *
     * @param filePath path to the input file
     * @return Map of username to User object
     * @throws IOException if file cannot be read
     */
    public Map<String, User> loadUsers(String filePath) throws IOException {
        Path path = Path.of(filePath);

        // Pre-size HashMap based on file line count estimate
        long fileSize = Files.size(path);
        int estimatedUsers = (int)(fileSize / 70); // Avg ~70 chars per line
        Map<String, User> users = new HashMap<>((int)(estimatedUsers * 1.25)); // Size with 0.8 load factor

        // Use BufferedReader for optimal I/O
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    User user = parseUserLine(line);
                    users.put(user.username(), user);
                }
            }
        }

        return users;
    }

    /**
     * Loads only the hashed passwords as a Set for O(1) lookup.
     * This is used for the HashSet-based optimization strategy.
     *
     * @param filePath path to the input file
     * @return Set of hashed passwords
     * @throws IOException if file cannot be read
     */
    public Set<String> loadHashSet(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::parseUserLine)
                .map(User::hashedPassword)
                .collect(Collectors.toSet());
    }

    /**
     * Parses a single line from the input file into a User object.
     *
     * @param line CSV line in format: username,hashedPassword
     * @return User object
     * @throws IllegalArgumentException if line format is invalid
     */
    private User parseUserLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid line format: " + line);
        }
        return new User(parts[0].trim(), parts[1].trim());
    }
}
