package org.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for loading dictionary passwords.
 * Follows Single Responsibility Principle - only handles dictionary loading.
 */
public class DictionaryLoaderService {

    /**
     * Loads all passwords from the dictionary file.
     * Filters out empty lines and trims whitespace.
     *
     * @param filePath path to the dictionary file
     * @return List of passwords
     * @throws IOException if file cannot be read
     */
    public List<String> loadDictionary(String filePath) throws IOException {
        return Files.lines(Path.of(filePath))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
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
        return Files.lines(Path.of(filePath))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .count();
    }
}
