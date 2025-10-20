package com.wego.carpark.repository;

import com.wego.carpark.model.CarParkAvailability;

import java.util.List;

/**
 * Custom repository interface for batch operations on CarParkAvailability.
 */
public interface CarParkAvailabilityRepositoryCustom {

    /**
     * Batch upsert car park availabilities using JDBC batch operations.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE for optimal performance.
     *
     * @param availabilities List of availabilities to upsert
     */
    void batchUpsertAvailabilities(List<CarParkAvailability> availabilities);
}