package org.example.service;

import org.example.model.CrackResult;
import org.example.model.CrackingStatistics;
import org.example.model.User;
import org.example.util.HashUtil;

import java.util.*;
import java.util.concurrent.*;

/**
 * High-performance concurrent password cracking engine.
 * Implements Strategy A: HashSet-based O(1) lookup with concurrent processing.
 * Uses fixed thread pool optimized for CPU-bound hashing tasks.
 */
public class PasswordCrackingEngine {

    private final int numThreads;

    /**
     * Creates a new PasswordCrackingEngine.
     * By default, uses number of available processors for optimal CPU utilization.
     */
    public PasswordCrackingEngine() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
    }

    /**
     * Creates a new PasswordCrackingEngine with specified thread count.
     *
     * @param numThreads number of threads to use
     */
    public PasswordCrackingEngine(int numThreads) {
        this.numThreads = numThreads;
    }

    /**
     * Cracks passwords using Strategy A: HashSet-based lookup.
     * Algorithm: O(N + M) where N = users, M = dictionary size
     * - Load all target hashes into Set: O(N)
     * - For each password, hash once and check if exists: O(M)
     * - Much faster than O(N*M) nested loop approach
     *
     * @param users       Map of users with their hashed passwords
     * @param dictionary  List of candidate passwords
     * @param statistics  Statistics collector for tracking progress
     * @return Map of username to CrackResult for successfully cracked passwords
     */
    public Map<String, CrackResult> crackPasswords(
            Map<String, User> users,
            List<String> dictionary,
            CrackingStatistics statistics
    ) throws InterruptedException {
        // Thread-safe map to store results
        Map<String, CrackResult> crackedPasswords = new ConcurrentHashMap<>();

        // Create reverse lookup: hash -> list of users (multiple users can have same password hash)
        Map<String, List<User>> hashToUsers = new ConcurrentHashMap<>();
        users.values().forEach(user ->
            hashToUsers.computeIfAbsent(user.hashedPassword(), k -> new ArrayList<>()).add(user)
        );

        // Create fixed thread pool for CPU-bound tasks
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Calculate chunk size for work distribution
        int chunkSize = Math.max(1, dictionary.size() / (numThreads * 4)); // 4x threads for better load balancing
        List<Future<?>> futures = new CopyOnWriteArrayList<>();

        // Partition dictionary and submit tasks
        for (int i = 0; i < dictionary.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, dictionary.size());
            List<String> chunk = dictionary.subList(start, end);

            Future<?> future = executor.submit(() ->
                    processPasswordChunk(chunk, hashToUsers, crackedPasswords, statistics)
            );
            futures.add(future);
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Error during password cracking", e.getCause());
            }
        }

        // Shutdown executor
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        return crackedPasswords;
    }

    /**
     * Processes a chunk of passwords from the dictionary.
     * Each thread processes its assigned chunk independently.
     *
     * @param passwordChunk    chunk of passwords to process
     * @param hashToUsers      reverse lookup map from hash to list of users
     * @param crackedPasswords map to store cracked passwords
     * @param statistics       statistics collector
     */
    private void processPasswordChunk(
            List<String> passwordChunk,
            Map<String, List<User>> hashToUsers,
            Map<String, CrackResult> crackedPasswords,
            CrackingStatistics statistics
    ) {
        for (String password : passwordChunk) {
            // Compute hash once for this password
            String hash = HashUtil.sha256(password);
            statistics.incrementHashesComputed();
            statistics.incrementTasksProcessed();

            // Check if this hash matches any users (O(1) lookup)
            List<User> matchedUsers = hashToUsers.get(hash);
            if (matchedUsers != null) {
                // Password cracked! Store result for ALL users with this hash
                for (User user : matchedUsers) {
                    crackedPasswords.putIfAbsent(
                            user.username(),
                            new CrackResult(user.username(), user.hashedPassword(), password)
                    );
                    statistics.incrementPasswordsFound();
                }
            }
        }
    }

    /**
     * Gets the number of threads being used.
     *
     * @return number of threads
     */
    public int getNumThreads() {
        return numThreads;
    }
}
