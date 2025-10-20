package com.wego.carpark.repository;

import com.wego.carpark.model.CarParkNotFound;

import java.util.List;

/**
 * Custom repository interface for batch operations on CarParkNotFound.
 */
public interface CarParkNotFoundRepositoryCustom {

    /**
     * Batch upsert car park not found records using JDBC batch operations.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE for optimal performance.
     *
     * @param notFoundRecords List of not found records to upsert
     */
    void batchUpsertNotFound(List<CarParkNotFound> notFoundRecords);
}