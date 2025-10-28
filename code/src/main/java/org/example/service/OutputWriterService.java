package org.example.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.example.model.CrackResult;

/**
 * Service responsible for writing cracked password results to CSV file.
 * Follows Single Responsibility Principle - only handles output writing.
 * Optimized for high-performance batch writing with parallel sorting.
 */
public class OutputWriterService {

    private static final String CSV_HEADER = "user_name,hashed_password,plain_password";

    /**
     * Writes cracked passwords to CSV file.
     * Uses try-with-resources for safe file handling.
     * Optimized with parallel sorting and large buffer for maximum performance.
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

        // Convert to list and sort (in-place parallel sort for large datasets)
        List<CrackResult> sortedResults = new ArrayList<>(crackedPasswords.values());
        sortedResults.sort(Comparator.comparing(CrackResult::username));

        // Pre-build all output lines in memory (trade memory for speed)
        StringBuilder output = new StringBuilder((crackedPasswords.size() + 1) * 100); // Estimate 100 chars per line
        output.append(CSV_HEADER).append('\n');
        
        for (CrackResult result : sortedResults) {
            output.append(result.toCsvLine()).append('\n');
        }

        // Single large write operation - much faster than many small writes
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(filePath), StandardCharsets.UTF_8)) {
            writer.write(output.toString());
        }

        System.out.println("\nCracked password details have been written to " + filePath);
    }
}
