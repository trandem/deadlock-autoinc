package com.wego.carpark.repository;

import com.wego.carpark.model.CarPark;

import java.util.List;

/**
 * Custom repository interface for batch operations on CarPark.
 */
public interface CarParkRepositoryCustom {

    /**
     * Batch upsert car park totals using JDBC batch operations.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE for optimal performance.
     *
     * @param carParks List of car parks to upsert
     */
    void batchUpsertCarParkTotals(List<CarPark> carParks);
}