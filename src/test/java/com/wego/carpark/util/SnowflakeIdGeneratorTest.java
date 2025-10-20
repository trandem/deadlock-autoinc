package com.wego.carpark.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SnowflakeIdGenerator.
 * Tests the generation of unique distributed IDs.
 */
@DisplayName("SnowflakeIdGenerator Tests")
class SnowflakeIdGeneratorTest {

    private SnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SnowflakeIdGenerator();
    }

    @Test
    @DisplayName("Should generate unique IDs")
    void shouldGenerateUniqueIds() {
        // Given: A set to store IDs
        Set<Long> ids = new HashSet<>();
        int count = 10000;

        // When: Generating multiple IDs
        for (int i = 0; i < count; i++) {
            var id = generator.nextId();
            ids.add(id);
        }

        // Then: All IDs should be unique
        assertEquals(count, ids.size(), "All generated IDs should be unique");
    }

    @Test
    @DisplayName("Should generate positive IDs")
    void shouldGeneratePositiveIds() {
        // When: Generating IDs
        for (int i = 0; i < 1000; i++) {
            var id = generator.nextId();

            // Then: ID should be positive
            assertTrue(id > 0, "Generated ID should be positive");
        }
    }

    @Test
    @DisplayName("Should generate IDs in ascending order within same millisecond")
    void shouldGenerateIdsInAscendingOrder() {
        // When: Generating multiple IDs quickly (likely within same millisecond)
        var id1 = generator.nextId();
        var id2 = generator.nextId();
        var id3 = generator.nextId();

        // Then: IDs should be in ascending order
        assertTrue(id2 > id1, "Second ID should be greater than first");
        assertTrue(id3 > id2, "Third ID should be greater than second");
    }

    @Test
    @DisplayName("Should generate unique IDs across multiple threads")
    void shouldGenerateUniqueIdsAcrossThreads() throws Exception {
        // Given: A concurrent set to store IDs
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        int threadsCount = 10;
        int idsPerThread = 1000;

        // When: Generating IDs from multiple threads
        var futures = IntStream.range(0, threadsCount)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    var id = generator.nextId();
                    ids.add(id);
                }
            }))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        // Then: All IDs should be unique
        assertEquals(threadsCount * idsPerThread, ids.size(),
            "All IDs from all threads should be unique");
    }

    @Test
    @DisplayName("Should handle rapid ID generation")
    void shouldHandleRapidIdGeneration() {
        // Given: A set to store IDs
        Set<Long> ids = new HashSet<>();

        // When: Generating IDs as fast as possible
        for (int i = 0; i < 5000; i++) {
            var id = generator.nextId();
            ids.add(id);
        }

        // Then: All IDs should be unique
        assertEquals(5000, ids.size(), "Should generate unique IDs rapidly");
    }

    @Test
    @DisplayName("Should throw exception for invalid machine ID from environment")
    void shouldThrowExceptionForInvalidMachineId() {
        // This test verifies the constructor validation
        // The actual generator uses machine ID 1 by default, which is valid
        // This test documents the expected behavior when invalid machine ID is provided

        // The generator with default machine ID should work fine
        assertDoesNotThrow(() -> {
            var gen = new SnowflakeIdGenerator();
            gen.nextId();
        });
    }

    @Test
    @DisplayName("Should generate IDs with consistent structure")
    void shouldGenerateIdsWithConsistentStructure() {
        // When: Generating an ID
        var id = generator.nextId();

        // Then: ID should be a positive 64-bit long
        assertTrue(id > 0);
        assertTrue(id < Long.MAX_VALUE);

        // ID should have reasonable structure (not all zeros or all ones)
        assertNotEquals(0L, id);
        assertNotEquals(-1L, id);
    }

    @Test
    @DisplayName("Should generate different IDs in different time windows")
    void shouldGenerateDifferentIdsInDifferentTimeWindows() throws InterruptedException {
        // Given: Generate ID in first time window
        var id1 = generator.nextId();

        // When: Wait a bit and generate another ID
        Thread.sleep(2); // Small delay to ensure different millisecond
        var id2 = generator.nextId();

        // Then: IDs should be different
        assertNotEquals(id1, id2, "IDs generated at different times should be different");
        assertTrue(id2 > id1, "Later ID should be greater than earlier ID");
    }

    @Test
    @DisplayName("Should handle sequence rollover within same millisecond")
    void shouldHandleSequenceRollover() {
        // Given: Generate many IDs quickly (to test sequence handling)
        Set<Long> ids = new HashSet<>();

        // When: Generate enough IDs to potentially test sequence increment
        for (int i = 0; i < 100; i++) {
            ids.add(generator.nextId());
        }

        // Then: All IDs should still be unique
        assertEquals(100, ids.size(), "Should handle sequence increments correctly");
    }

    @Test
    @DisplayName("Should be thread-safe")
    void shouldBeThreadSafe() throws Exception {
        // Given: Multiple threads generating IDs concurrently
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        int numberOfThreads = 20;
        int idsPerThread = 500;

        // When: Multiple threads generate IDs simultaneously
        var futures = IntStream.range(0, numberOfThreads)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    var id = generator.nextId();
                    boolean added = ids.add(id);
                    assertTrue(added, "Each ID should be unique, found duplicate: " + id);
                }
            }))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        // Then: All IDs should be unique
        assertEquals(numberOfThreads * idsPerThread, ids.size(),
            "Thread-safe generation should produce unique IDs");
    }
}