package com.wego.carpark.service;

import com.wego.carpark.dto.CarParkData;
import com.wego.carpark.dto.CarParkInfo;
import com.wego.carpark.model.CarPark;
import com.wego.carpark.model.CarParkAvailability;
import com.wego.carpark.model.CarParkNotFound;
import com.wego.carpark.repository.CarParkAvailabilityRepository;
import com.wego.carpark.repository.CarParkNotFoundRepository;
import com.wego.carpark.repository.CarParkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service to process a shard of car park availability data in a single transaction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CarParkAvailabilityShardProcessor {

    private final CarParkRepository carParkRepository;
    private final CarParkAvailabilityRepository availabilityRepository;
    private final CarParkNotFoundRepository carParkNotFoundRepository;

    /**
     * Process a shard of car park data with batch query and batch update.
     * All operations are done in a single transaction.
     */
    @Transactional
    public ShardResult processShard(List<CarParkData> shard, LocalDateTime updateTime) {
        // Step 1: Collect all car park numbers in this shard
        List<String> carParkNos = shard.stream()
                .map(CarParkData::getCarparkNumber)
                .toList();

        // Step 2: Batch query car parks from database
        List<CarPark> carParks = carParkRepository.findByCarParkNoIn(carParkNos);
        Map<String, CarPark> carParkMap = new HashMap<>();
        for (CarPark carPark : carParks) {
            carParkMap.put(carPark.getCarParkNo(), carPark);
        }

        // Step 3: Process all records
        List<CarParkAvailability> availabilitiesToSave = new ArrayList<>();
        List<String> notFoundCarParkNos = new ArrayList<>();
        Map<Long, Map<String, Integer>> carParkAggregates = new HashMap<>();
        int processedCount = 0;

        for (CarParkData carParkData : shard) {
            String carParkNo = carParkData.getCarparkNumber();
            List<CarParkInfo> carParkInfoList = carParkData.getCarparkInfo();

            if (carParkInfoList == null || carParkInfoList.isEmpty()) {
                continue;
            }

            CarPark carPark = carParkMap.get(carParkNo);
            if (carPark == null) {
                notFoundCarParkNos.add(carParkNo);
                continue;
            }

            int totalLotsSum = 0;
            int availableLotsSum = 0;

            // Process each lot type
            for (CarParkInfo info : carParkInfoList) {
                String lotType = info.getLotType();
                int totalLots = info.getTotalLots();
                int lotsAvailable = info.getLotsAvailable();

                // Find existing or create new availability record
                Optional<CarParkAvailability> existingOpt = availabilityRepository
                        .findByCarParkIdAndLotType(carPark.getId(), lotType);

                CarParkAvailability availability;
                if (existingOpt.isPresent()) {
                    availability = existingOpt.get();
                    availability.setTotalLots(totalLots);
                    availability.setAvailableLots(lotsAvailable);
                    availability.setUpdateDatetime(updateTime);
                } else {
                    availability = CarParkAvailability.builder()
                            .carParkId(carPark.getId())
                            .carParkNo(carParkNo)
                            .lotType(lotType)
                            .totalLots(totalLots)
                            .availableLots(lotsAvailable)
                            .updateDatetime(updateTime)
                            .build();
                }

                availabilitiesToSave.add(availability);
                processedCount++;

                // Aggregate totals
                totalLotsSum += totalLots;
                availableLotsSum += lotsAvailable;
            }

            // Store aggregates for car_parks table update
            Map<String, Integer> aggregates = new HashMap<>();
            aggregates.put("totalLots", totalLotsSum);
            aggregates.put("availableLots", availableLotsSum);
            carParkAggregates.put(carPark.getId(), aggregates);
        }

        // Step 4: Batch save availability records
        if (!availabilitiesToSave.isEmpty()) {
            availabilityRepository.saveAll(availabilitiesToSave);
            log.debug("Saved {} availability records", availabilitiesToSave.size());
        }

        // Step 5: Batch update car_parks table
        for (Map.Entry<Long, Map<String, Integer>> entry : carParkAggregates.entrySet()) {
            Long carParkId = entry.getKey();
            Map<String, Integer> aggregates = entry.getValue();
            carParkRepository.updateTotalLotsAndAvailableLots(
                    carParkId,
                    aggregates.get("totalLots"),
                    aggregates.get("availableLots")
            );
        }
        log.debug("Updated {} car parks with aggregated totals", carParkAggregates.size());

        // Step 6: Save not found car parks
        if (!notFoundCarParkNos.isEmpty()) {
            for (String carParkNo : notFoundCarParkNos) {
                Optional<CarParkNotFound> existingOpt = carParkNotFoundRepository.findByCarParkNo(carParkNo);

                if (existingOpt.isPresent()) {
                    CarParkNotFound existing = existingOpt.get();
                    existing.setLastSeenAt(updateTime);
                    existing.setOccurrenceCount(existing.getOccurrenceCount() + 1);
                    carParkNotFoundRepository.save(existing);
                } else {
                    CarParkNotFound notFound = CarParkNotFound.builder()
                            .carParkNo(carParkNo)
                            .firstSeenAt(updateTime)
                            .lastSeenAt(updateTime)
                            .occurrenceCount(1)
                            .build();
                    carParkNotFoundRepository.save(notFound);
                }
            }
            log.debug("Saved {} not found car parks", notFoundCarParkNos.size());
        }

        return new ShardResult(processedCount, notFoundCarParkNos.size());
    }

    /**
     * Result of processing a shard.
     */
    public static class ShardResult {
        public final int processedCount;
        public final int notFoundCount;

        public ShardResult(int processedCount, int notFoundCount) {
            this.processedCount = processedCount;
            this.notFoundCount = notFoundCount;
        }
    }
}