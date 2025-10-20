package com.wego.carpark.service;

import com.wego.carpark.dto.CarParkData;
import com.wego.carpark.dto.CarParkInfo;
import com.wego.carpark.model.CarPark;
import com.wego.carpark.model.CarParkAvailability;
import com.wego.carpark.repository.CarParkAvailabilityRepository;
import com.wego.carpark.repository.CarParkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to process a shard of car park availability data.
 * Uses CarParkDataPersistenceService for transactional batch saves.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CarParkAvailabilityShardProcessor {

    private final CarParkRepository carParkRepository;
    private final CarParkAvailabilityRepository availabilityRepository;
    private final CarParkDataPersistenceService persistenceService;

    /**
     * Process a shard of car park data with batch query and batch update.
     * Separates data building from transactional saving.
     */
    public ShardResult processShard(List<CarParkData> shard, LocalDateTime updateTime) {
        // Build all data structures
        var processedData = buildProcessedData(shard, updateTime);

        // Save everything in a single transaction
        persistenceService.saveProcessedData(
                processedData.availabilitiesToSave(),
                processedData.carParksToUpdate()
        );

        return new ShardResult(processedData.processedCount());
    }

    /**
     * Build all data structures needed for processing.
     * Uses batch query to minimize database round trips.
     * Fetches existing records to reuse their IDs for proper updates.
     */
    private ProcessedShardData buildProcessedData(List<CarParkData> shard, LocalDateTime updateTime) {
        var carParkNos = shard.stream()
                .map(CarParkData::getCarparkNumber)
                .toList();

        var carParkMap = buildCarParkLookupMap(carParkNos);
        var availabilityMap = buildAvailabilityLookupMap(carParkMap.values());

        var availabilitiesToSave = new ArrayList<CarParkAvailability>();
        var carParksToUpdate = new ArrayList<CarPark>();
        var processedCount = 0;

        for (var carParkData : shard) {
            var result = processCarParkData(carParkData, carParkMap, availabilityMap, updateTime);
            if (result != null) {
                availabilitiesToSave.addAll(result.availabilities());
                carParksToUpdate.add(result.carPark());
                processedCount += result.availabilities().size();
            }
        }

        return new ProcessedShardData(availabilitiesToSave, carParksToUpdate, processedCount);
    }

    /**
     * Build lookup map of car parks by car park number.
     */
    private Map<String, CarPark> buildCarParkLookupMap(List<String> carParkNos) {
        return carParkRepository.findByCarParkNoIn(carParkNos).stream()
                .collect(Collectors.toMap(CarPark::getCarParkNo, carPark -> carPark));
    }

    /**
     * Build lookup map of existing availability records by "carParkId:lotType" key.
     */
    private Map<String, CarParkAvailability> buildAvailabilityLookupMap(Collection<CarPark> carParks) {
        var carParkIds = carParks.stream()
                .map(CarPark::getId)
                .toList();

        return availabilityRepository.findByCarParkIdIn(carParkIds).stream()
                .collect(Collectors.toMap(
                        availability -> availability.getCarParkId() + ":" + availability.getLotType(),
                        availability -> availability
                ));
    }

    /**
     * Process a single car park's availability data.
     * Returns null if car park not found or has no valid data.
     */
    private CarParkProcessingResult processCarParkData(
            CarParkData carParkData,
            Map<String, CarPark> carParkMap,
            Map<String, CarParkAvailability> availabilityMap,
            LocalDateTime updateTime) {

        var carParkNo = carParkData.getCarparkNumber();
        var carParkInfoList = carParkData.getCarparkInfo();

        if (carParkInfoList == null || carParkInfoList.isEmpty()) {
            return null;
        }

        var carPark = carParkMap.get(carParkNo);
        if (carPark == null) {
            log.warn("Car park not found in database for carParkNo={}", carParkNo);
            return null;
        }

        var availabilities = new ArrayList<CarParkAvailability>();
        var totalLotsSum = 0;
        var availableLotsSum = 0;

        for (var info : carParkInfoList) {
            var availability = createAvailabilityRecord(carPark, info, availabilityMap, updateTime);
            availabilities.add(availability);

            totalLotsSum += info.getTotalLots();
            availableLotsSum += info.getLotsAvailable();
        }

        carPark.setTotalLots(totalLotsSum);
        carPark.setTotalAvailableLots(availableLotsSum);

        return new CarParkProcessingResult(carPark, availabilities);
    }

    /**
     * Create availability record, reusing existing ID if found.
     */
    private CarParkAvailability createAvailabilityRecord(
            CarPark carPark,
            CarParkInfo info,
            Map<String, CarParkAvailability> availabilityMap,
            LocalDateTime updateTime) {

        var availabilityKey = carPark.getId() + ":" + info.getLotType();
        var existingAvailability = availabilityMap.get(availabilityKey);

        return CarParkAvailability.builder()
                .id(existingAvailability != null ? existingAvailability.getId() : null)
                .carParkId(carPark.getId())
                .carParkNo(carPark.getCarParkNo())
                .lotType(info.getLotType())
                .totalLots(info.getTotalLots())
                .availableLots(info.getLotsAvailable())
                .updateDatetime(updateTime)
                .build();
    }

    /**
     * Result of processing a single car park.
     */
    private record CarParkProcessingResult(CarPark carPark, List<CarParkAvailability> availabilities) {}


    /**
     * Data structure to hold all processed data for a shard before saving.
     * Contains 3 lists for batch upserts to database.
     */
    public record ProcessedShardData(
            List<CarParkAvailability> availabilitiesToSave,
            List<CarPark> carParksToUpdate,
            int processedCount
    ) {}

    /**
     * Result of processing a shard.
     */
    public record ShardResult(int processedCount) {}
}