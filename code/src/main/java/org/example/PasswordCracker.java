package org.example;

import java.util.List;
import java.util.Map;

import org.example.model.CrackResult;
import org.example.model.CrackingStatistics;
import org.example.model.User;
import org.example.service.DictionaryLoaderService;
import org.example.service.HashLoaderService;
import org.example.service.OutputWriterService;
import org.example.service.PasswordCrackingEngine;
import org.example.service.StatusReporterService;

/**
 * Main orchestrator for password dictionary attack application.
 * Coordinates all services to load data, crack passwords, report status, and write results.
 *
 * This refactored version addresses all requirements:
 * - Architectural: Deconstructed monolith into multiple components following SOLID principles
 * - Performance: O(N+M) algorithm with HashSet lookup, high-performance concurrency
 * - Concurrency: Thread-safe with AtomicLong counters, ExecutorService thread pool
 * - Modernization: Uses Java Records, Streams API, modern collection interfaces
 * - Feature: Separate thread for non-blocking status updates
 */
public class PasswordCracker {

    private final HashLoaderService hashLoader;
    private final DictionaryLoaderService dictionaryLoader;
    private final PasswordCrackingEngine crackingEngine;
    private final OutputWriterService outputWriter;

    /**
     * Creates a new PasswordCracker with default configuration.
     */
    public PasswordCracker() {
        this.hashLoader = new HashLoaderService();
        this.dictionaryLoader = new DictionaryLoaderService();

        // Allow thread count to be configured via system property
        int numThreads = Integer.getInteger("num.threads", 4);
        this.crackingEngine = new PasswordCrackingEngine(numThreads);
        this.outputWriter = new OutputWriterService();
    }

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments: [input_file] [dictionary_file] [output_file]
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar run.jar <input_file> <dictionary_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String dictionaryFile = args[1];
        String outputFile = args[2];

        var cracker = new PasswordCracker();
        try {
            cracker.execute(inputFile, dictionaryFile, outputFile);
        } catch (Exception e) {
            System.err.println("Error during password cracking: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Executes the password cracking process.
     *
     * @param inputFile      path to input file with users and hashes
     * @param dictionaryFile path to dictionary file with passwords
     * @param outputFile     path to output file for cracked passwords
     * @throws Exception if any error occurs during execution
     */
    public void execute(String inputFile, String dictionaryFile, String outputFile) throws Exception {
        long overallStart = System.currentTimeMillis();
        
        System.out.println("=== Password Dictionary Attack ===");
        System.out.println("Loading data...");

        // Load users and dictionary
        long loadStart = System.currentTimeMillis();
        Map<String, User> users = hashLoader.loadUsers(inputFile);
        long usersLoadTime = System.currentTimeMillis() - loadStart;
        
        long dictStart = System.currentTimeMillis();
        List<String> dictionary = dictionaryLoader.loadDictionary(dictionaryFile);
        long dictLoadTime = System.currentTimeMillis() - dictStart;

        System.out.println("Loaded " + users.size() + " users in " + usersLoadTime + "ms");
        System.out.println("Loaded " + dictionary.size() + " passwords in " + dictLoadTime + "ms");

        // Calculate total tasks for statistics
        long totalTasks = dictionary.size(); // One task per password (much better than N*M)

        // Initialize statistics tracker
        var statistics = new CrackingStatistics(totalTasks);

        // Start status reporter in separate thread
        var statusReporter = new StatusReporterService(statistics);
        statusReporter.start();

        System.out.println("\nStarting attack with " + totalTasks + " total tasks...");
        System.out.println("Using " + crackingEngine.getNumThreads() + " threads for parallel processing\n");

        // Execute password cracking
        long crackStart = System.currentTimeMillis();
        Map<String, CrackResult> crackedPasswords = crackingEngine.crackPasswords(
                users,
                dictionary,
                statistics
        );
        long crackTime = System.currentTimeMillis() - crackStart;

        // Stop status reporter
        statusReporter.stop();

        // Print final summary
        statusReporter.printFinalSummary();

        // Write results to output file
        long writeStart = System.currentTimeMillis();
        if (!crackedPasswords.isEmpty()) {
            outputWriter.writeCrackedPasswords(outputFile, crackedPasswords);
        } else {
            System.out.println("No passwords were cracked.");
        }
        long writeTime = System.currentTimeMillis() - writeStart;

        long overallTime = System.currentTimeMillis() - overallStart;
        
        System.out.println("\n=== Performance Breakdown ===");
        System.out.printf("User Loading:     %5dms (%5.1f%%)%n", usersLoadTime, usersLoadTime * 100.0 / overallTime);
        System.out.printf("Dict Loading:     %5dms (%5.1f%%)%n", dictLoadTime, dictLoadTime * 100.0 / overallTime);
        System.out.printf("Password Cracking:%5dms (%5.1f%%) ⭐%n", crackTime, crackTime * 100.0 / overallTime);
        System.out.printf("Output Writing:   %5dms (%5.1f%%)%n", writeTime, writeTime * 100.0 / overallTime);
        System.out.printf("Total Time:       %5dms%n", overallTime);
        
        System.out.println("\n=== Attack Complete ===");
    }
}
