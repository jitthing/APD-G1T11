package org.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for loading dictionary passwords.
 * Follows Single Responsibility Principle - only handles dictionary loading.
 */
public class DictionaryLoaderService {

    /**
     * Loads all passwords from the dictionary file.
     * Optimized with BufferedReader and pre-sized list.
     *
     * @param filePath path to the dictionary file
     * @return List of passwords
     * @throws IOException if file cannot be read
     */
    public List<String> loadDictionary(String filePath) throws IOException {
        Path path = Path.of(filePath);

        // Pre-size list based on file size estimate (avg 10 chars per password)
        long fileSize = Files.size(path);
        int estimatedCapacity = (int)(fileSize / 10);
        List<String> passwords = new ArrayList<>(estimatedCapacity);

        // Use BufferedReader with 64KB buffer for optimal I/O
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    passwords.add(line);
                }
            }
        }

        return passwords;
    }

    /**
     * Loads dictionary and returns the count of passwords.
     * Useful for statistics and progress tracking.
     *
     * @param filePath path to the dictionary file
     * @return number of passwords in dictionary
     * @throws IOException if file cannot be read
     */
    public long getDictionarySize(String filePath) throws IOException {
        long count = 0;
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }
}
