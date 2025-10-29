package org.example.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.example.model.CrackResult;
import org.example.model.CrackingStatistics;
import org.example.model.User;
import org.example.util.HashUtil;

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

        // Use work-stealing pool for better load balancing with CPU-bound tasks
        // This automatically balances work across threads more efficiently
        ExecutorService executor = Executors.newWorkStealingPool(numThreads);

        // Optimal chunk size: exactly numThreads chunks for less overhead
        // This reduces context switching and synchronization costs
        int chunkSize = (dictionary.size() + numThreads - 1) / numThreads;
        List<Future<?>> futures = new ArrayList<>(numThreads);

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
     * Uses local counters to minimize atomic contention, then batch updates at the end.
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
        // Local counters to avoid atomic contention in hot loop
        // These will be batch-updated at the end
        long localHashesComputed = 0;
        long localTasksProcessed = 0;
        long localPasswordsFound = 0;

        for (String password : passwordChunk) {
            // Compute hash once for this password
            String hash = HashUtil.sha256(password);
            localHashesComputed++;
            localTasksProcessed++;

            // Check if this hash matches any users (O(1) lookup)
            List<User> matchedUsers = hashToUsers.get(hash);
                if (matchedUsers != null) {
                    // Password cracked! Store result for ALL users with this hash
                    // Only count a password as "found" when we actually insert a new entry
                    // into the crackedPasswords map. This avoids over-counting when the
                    // dictionary contains duplicate candidate passwords (the same hash
                    // may be matched multiple times).
                    for (User user : matchedUsers) {
                        CrackResult previous = crackedPasswords.putIfAbsent(
                                user.username(),
                                new CrackResult(user.username(), user.hashedPassword(), password)
                        );
                        if (previous == null) {
                            // We successfully added a new cracked user entry
                            localPasswordsFound++;
                        }
                    }
                }
        }

        // Batch update: single atomic operation per counter instead of thousands
        statistics.addHashesComputed(localHashesComputed);
        statistics.addTasksProcessed(localTasksProcessed);
        statistics.addPasswordsFound(localPasswordsFound);
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
