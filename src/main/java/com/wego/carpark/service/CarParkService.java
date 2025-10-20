package com.wego.carpark.service;

import com.wego.carpark.dto.CarParkResponseDto;
import com.wego.carpark.mapper.CarParkMapper;
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
    public List<CarParkResponseDto> findNearestCarParks(
            Double latitude,
            Double longitude,
            Integer page,
            Integer perPage) {

        log.info("Finding nearest car parks for lat: {}, lon: {}, page: {}, perPage: {}",
                latitude, longitude, page, perPage);

        var start = Instant.now();

        // Convert page from 1-indexed (API) to 0-indexed
        var pageIndex = page - 1;
        var offset = pageIndex * perPage;

        // Get results from both threads
        var carParks = carParkRepository.findNearestCarParksWithAvailabilityAsList(
                latitude, longitude, perPage, offset);

        var duration = Duration.between(start, Instant.now());
        log.info("Found {} car parks in {}ms using PARALLEL queries with virtual threads",
                carParks.size(), duration.toMillis());

        // Convert to DTOs
        return carParks.stream()
                .map(carParkMapper::toDto)
                .collect(Collectors.toList());
    }
}
