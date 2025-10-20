package com.wego.carpark.util;

import org.springframework.stereotype.Component;

/**
 * Snowflake ID generator for distributed unique ID generation.
 * Generates 64-bit unique IDs based on Twitter's Snowflake algorithm.
 *
 * Structure: 1 bit (unused) | 41 bits (timestamp) | 10 bits (machine ID) | 12 bits (sequence)
 */
@Component
public class SnowflakeIdGenerator {

    // Epoch (2024-01-01 00:00:00 UTC in milliseconds)
    private static final long EPOCH = 1704067200000L;

    // Bit lengths
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    // Max values
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // Shifts
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = MACHINE_ID_BITS + SEQUENCE_BITS;

    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        // Use a machine ID based on environment or default to 1
        // In production, you should set this via configuration (e.g., from pod number, server ID, etc.)
        var machineIdFromEnv = System.getenv("MACHINE_ID");
        this.machineId = machineIdFromEnv != null
            ? Long.parseLong(machineIdFromEnv)
            : 1L;

        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException(
                String.format("Machine ID must be between 0 and %d", MAX_MACHINE_ID));
        }
    }

    /**
     * Generate a unique Snowflake ID.
     * Thread-safe method that generates unique 64-bit IDs.
     */
    public synchronized long nextId() {
        var timestamp = currentTimeMillis();

        // Clock moved backwards
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds",
                    lastTimestamp - timestamp));
        }

        // Same millisecond - increment sequence
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // Sequence exhausted - wait for next millisecond
            if (sequence == 0) {
                timestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            // New millisecond - reset sequence
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // Generate ID: timestamp | machineId | sequence
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
            | (machineId << MACHINE_ID_SHIFT)
            | sequence;
    }

    /**
     * Wait until next millisecond.
     */
    private long waitForNextMillis(long lastTimestamp) {
        var timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * Get current timestamp in milliseconds.
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
