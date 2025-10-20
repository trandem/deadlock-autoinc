package com.wego.carpark.service;

import com.wego.carpark.model.CarPark;
import com.wego.carpark.model.CarParkAvailability;
import com.wego.carpark.repository.CarParkAvailabilityRepository;
import com.wego.carpark.repository.CarParkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for persisting car park data in a transactional manner.
 * Separated from the processor to ensure @Transactional works correctly.
 * Spring's @Transactional doesn't work with self-invocation (internal method calls).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CarParkDataPersistenceService {

    private final CarParkRepository carParkRepository;
    private final CarParkAvailabilityRepository availabilityRepository;

    /**
     * Save all processed data in a single transaction using batch upserts.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE for optimal performance.
     *
     * @param availabilities List of availability records to upsert
     * @param carParks List of car parks to upsert with updated totals
     */
    @Transactional
    public void saveProcessedData(List<CarParkAvailability> availabilities,
                                   List<CarPark> carParks) {
        // Step 1: Batch upsert availability records
        if (!availabilities.isEmpty()) {
            availabilityRepository.batchUpsertAvailabilities(availabilities);
            log.debug("Batch upserted {} availability records", availabilities.size());
        }

        // Step 2: Batch upsert car_parks table totals
        if (!carParks.isEmpty()) {
            carParkRepository.batchUpsertCarParkTotals(carParks);
            log.debug("Batch upserted {} car parks with aggregated totals", carParks.size());
        }
    }
}
