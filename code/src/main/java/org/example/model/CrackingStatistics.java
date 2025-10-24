package org.example.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics collector for password cracking operations.
 * Uses atomic operations to prevent race conditions in concurrent environment.
 */
public class CrackingStatistics {
    private final AtomicLong passwordsFound = new AtomicLong(0);
    private final AtomicLong hashesComputed = new AtomicLong(0);
    private final AtomicLong tasksProcessed = new AtomicLong(0);
    private final long totalTasks;
    private final long startTime;

    /**
     * Creates a new statistics collector.
     *
     * @param totalTasks total number of tasks to process
     */
    public CrackingStatistics(long totalTasks) {
        this.totalTasks = totalTasks;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Increments the count of passwords found.
     */
    public void incrementPasswordsFound() {
        passwordsFound.incrementAndGet();
    }

    /**
     * Increments the count of hashes computed.
     */
    public void incrementHashesComputed() {
        hashesComputed.incrementAndGet();
    }

    /**
     * Increments the count of tasks processed.
     */
    public void incrementTasksProcessed() {
        tasksProcessed.incrementAndGet();
    }

    /**
     * Gets the current count of passwords found.
     *
     * @return number of passwords cracked
     */
    public long getPasswordsFound() {
        return passwordsFound.get();
    }

    /**
     * Gets the current count of hashes computed.
     *
     * @return number of hashes computed
     */
    public long getHashesComputed() {
        return hashesComputed.get();
    }

    /**
     * Gets the current count of tasks processed.
     *
     * @return number of tasks completed
     */
    public long getTasksProcessed() {
        return tasksProcessed.get();
    }

    /**
     * Gets the total number of tasks.
     *
     * @return total tasks
     */
    public long getTotalTasks() {
        return totalTasks;
    }

    /**
     * Gets remaining tasks to process.
     *
     * @return tasks remaining
     */
    public long getRemainingTasks() {
        return totalTasks - tasksProcessed.get();
    }

    /**
     * Calculates the current progress percentage.
     *
     * @return progress as percentage (0-100)
     */
    public double getProgressPercent() {
        if (totalTasks == 0) return 100.0;
        return (double) tasksProcessed.get() / totalTasks * 100.0;
    }

    /**
     * Gets elapsed time in milliseconds.
     *
     * @return elapsed time since start
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Gets the start time.
     *
     * @return start timestamp in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }
}
