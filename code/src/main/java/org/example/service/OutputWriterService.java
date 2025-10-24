package org.example.service;

import org.example.model.CrackResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Service responsible for writing cracked password results to CSV file.
 * Follows Single Responsibility Principle - only handles output writing.
 */
public class OutputWriterService {

    private static final String CSV_HEADER = "user_name,hashed_password,plain_password";

    /**
     * Writes cracked passwords to CSV file.
     * Uses try-with-resources for safe file handling.
     *
     * @param filePath         path to output CSV file
     * @param crackedPasswords map of cracked passwords
     * @throws IOException if file cannot be written
     */
    public void writeCrackedPasswords(String filePath, Map<String, CrackResult> crackedPasswords) throws IOException {
        if (crackedPasswords.isEmpty()) {
            System.out.println("No passwords were cracked. Output file not created.");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(filePath))) {
            // Write CSV header
            writer.write(CSV_HEADER);
            writer.newLine();

            // Write each cracked password result
            crackedPasswords.values().stream()
                    .sorted((a, b) -> a.username().compareTo(b.username())) // Sort by username for consistency
                    .forEach(result -> {
                        try {
                            writer.write(result.toCsvLine());
                            writer.newLine();
                        } catch (IOException e) {
                            throw new RuntimeException("Error writing crack result: " + result, e);
                        }
                    });
        }

        System.out.println("\nCracked password details have been written to " + filePath);
    }
}
