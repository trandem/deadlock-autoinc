package com.wego.carpark.service;

import com.wego.carpark.dto.CarParkResponseDto;
import com.wego.carpark.mapper.CarParkMapper;
import com.wego.carpark.model.CarPark;
import com.wego.carpark.repository.CarParkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Service for handling car park business logic.
 * Provides methods to find nearest car parks with availability.
 * Uses virtual threads for parallel database operations.
 */
@Service
@Slf4j
public class CarParkService {

    private final CarParkRepository carParkRepository;
    private final CarParkMapper carParkMapper;
    private final ExecutorService virtualThreadExecutor;

    /**
     * Constructor with virtual thread executor for parallel database operations.
     */
    public CarParkService(
            CarParkRepository carParkRepository,
            CarParkMapper carParkMapper,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.carParkRepository = carParkRepository;
        this.carParkMapper = carParkMapper;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Find nearest car parks with available lots.
     * Uses PARALLEL execution with virtual threads:
     * - COUNT query runs in one virtual thread
     * - SELECT query runs in another virtual thread
     * Both queries execute concurrently for better performance!
     *
     * @param latitude  User's latitude
     * @param longitude User's longitude
     * @param page      Page number (1-indexed for API, but 0-indexed for repository)
     * @param perPage   Number of results per page
     * @return List of car parks with availability, sorted by distance
     */
    @Transactional(readOnly = true)
    public List<CarParkResponseDto> findNearestCarParks(
            Double latitude,
            Double longitude,
            Integer page,
            Integer perPage) {

        log.info("Finding nearest car parks for lat: {}, lon: {}, page: {}, perPage: {}",
                latitude, longitude, page, perPage);

        Instant start = Instant.now();

        // Convert page from 1-indexed (API) to 0-indexed
        int pageIndex = page - 1;
        int offset = pageIndex * perPage;

        // ===== PARALLEL EXECUTION USING VIRTUAL THREADS =====

        // Thread 1: Execute COUNT query
        CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(() -> {
            Thread currentThread = Thread.currentThread();
            log.debug("COUNT query running on thread: {} (virtual={})",
                    currentThread.getName(), currentThread.isVirtual());

            long count = carParkRepository.countCarParksWithAvailability();

            log.debug("COUNT query completed: {} total car parks", count);
            return count;
        }, virtualThreadExecutor);

        // Thread 2: Execute SELECT query
        CompletableFuture<List<CarPark>> selectFuture = CompletableFuture.supplyAsync(() -> {
            Thread currentThread = Thread.currentThread();
            log.debug("SELECT query running on thread: {} (virtual={})",
                    currentThread.getName(), currentThread.isVirtual());

            List<CarPark> carParks = carParkRepository.findNearestCarParksWithAvailabilityAsList(
                    latitude, longitude, perPage, offset);

            log.debug("SELECT query completed: {} car parks in current page", carParks.size());
            return carParks;
        }, virtualThreadExecutor);

        // Wait for BOTH queries to complete (they run in parallel!)
        CompletableFuture.allOf(countFuture, selectFuture).join();

        // Get results from both threads
        Long totalCount = countFuture.join();
        List<CarPark> carParks = selectFuture.join();

        Duration duration = Duration.between(start, Instant.now());
        log.info("Found {} car parks (total: {}) in {}ms using PARALLEL queries with virtual threads",
                carParks.size(), totalCount, duration.toMillis());

        // Convert to DTOs
        return carParks.stream()
                .map(carParkMapper::toDto)
                .collect(Collectors.toList());
    }
}
