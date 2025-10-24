package org.example.service;

import org.example.model.CrackingStatistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service that provides non-blocking, live status updates in a separate thread.
 * Uses ScheduledExecutorService for periodic updates without blocking main processing.
 * Follows Single Responsibility Principle - only handles status reporting.
 */
public class StatusReporterService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int UPDATE_INTERVAL_MS = 100; // Update every 100ms as per spec

    private final CrackingStatistics statistics;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    /**
     * Creates a new StatusReporterService.
     *
     * @param statistics the statistics object to read from
     */
    public StatusReporterService(CrackingStatistics statistics) {
        this.statistics = statistics;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "StatusReporter");
            thread.setDaemon(true); // Daemon thread won't prevent JVM shutdown
            return thread;
        });
    }

    /**
     * Starts the status reporter in a separate thread.
     * Updates are printed every 100ms to match original specification.
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        scheduler.scheduleAtFixedRate(
                this::printStatus,
                0,
                UPDATE_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the status reporter and shuts down the scheduler.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Prints the current status to console.
     * Uses carriage return (\r) for in-place updates as in original code.
     */
    private void printStatus() {
        // Only print if we have tasks to process (avoid division by zero and unnecessary output)
        if (statistics.getTotalTasks() > 0 && statistics.getTasksProcessed() % 1000 == 0) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            double progressPercent = statistics.getProgressPercent();
            long passwordsFound = statistics.getPasswordsFound();
            long remainingTasks = statistics.getRemainingTasks();

            System.out.printf("\r[%s] %.2f%% complete | Passwords Found: %d | Tasks Remaining: %d",
                    timestamp, progressPercent, passwordsFound, remainingTasks);
            System.out.flush(); // Ensure output is displayed immediately
        }
    }

    /**
     * Prints a final summary after cracking is complete.
     */
    public void printFinalSummary() {
        System.out.println(); // New line after progress updates
        System.out.println("Total passwords found: " + statistics.getPasswordsFound());
        System.out.println("Total hashes computed: " + statistics.getHashesComputed());
        System.out.println("Total time spent (milliseconds): " + statistics.getElapsedTime());
    }
}
