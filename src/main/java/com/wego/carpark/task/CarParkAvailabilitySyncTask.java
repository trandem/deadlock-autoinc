package com.wego.carpark.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.carpark.dto.CarParkAvailabilityApiResponse;
import com.wego.carpark.dto.CarParkData;
import com.wego.carpark.dto.CarParkDataItem;
import com.wego.carpark.dto.CarParkInfo;
import com.wego.carpark.service.CarParkAvailabilityShardProcessor;
import com.wego.carpark.service.CarParkAvailabilityShardProcessor.ShardResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task to sync car park availability from the external API.
 * This can be run manually or scheduled to update availability data periodically.
 *
 * API Endpoint: https://api.data.gov.sg/v1/transport/carpark-availability
 */
@Component
@Slf4j
public class CarParkAvailabilitySyncTask {

    private final CarParkAvailabilityShardProcessor shardProcessor;
    private final ExecutorService virtualThreadExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String API_URL = "https://api.data.gov.sg/v1/transport/carpark-availability";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final int SHARD_SIZE = 200; // Number of records per shard

    /**
     * Constructor with dependencies injection.
     */
    public CarParkAvailabilitySyncTask(
            CarParkAvailabilityShardProcessor shardProcessor,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.shardProcessor = shardProcessor;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Sync availability data from the external API.
     * This method can be called manually or scheduled.
     */
    public void syncAvailabilityData() {
        log.info("Starting car park availability sync...");

        try {
            String jsonResponse = fetchAvailabilityData();
            int updatedCount = processAvailabilityData(jsonResponse);
            log.info("Car park availability sync completed. Updated {} records.", updatedCount);
        } catch (Exception e) {
            log.error("Error syncing car park availability data", e);
            throw new RuntimeException("Failed to sync availability data", e);
        }
    }

    /**
     * Fetch availability data from the API.
     * Uses a reusable HttpClient instance for better performance.
     */
    private String fetchAvailabilityData() throws IOException, InterruptedException {
        log.info("Fetching data from: {}", API_URL);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status code: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Process and save availability data using sharding with batch processing.
     * Data is divided into shards of SHARD_SIZE (200 records each).
     * Deduplicates car park numbers to prevent deadlocks.
     */
    private int processAvailabilityData(String jsonResponse) throws Exception {
        CarParkAvailabilityApiResponse apiResponse = objectMapper.readValue(jsonResponse, CarParkAvailabilityApiResponse.class);

        if (apiResponse.getItems() == null || apiResponse.getItems().isEmpty()) {
            log.warn("No data items found in API response");
            return 0;
        }

        // Get the first (most recent) item
        CarParkDataItem firstItem = apiResponse.getItems().get(0);
        LocalDateTime updateTime = LocalDateTime.parse(firstItem.getTimestamp(), DATE_FORMATTER);

        List<CarParkData> carParkDataList = firstItem.getCarparkData();
        if (carParkDataList == null || carParkDataList.isEmpty()) {
            log.warn("No carpark_data found in API response");
            return 0;
        }

        // Group and merge car parks by carParkNo to prevent deadlocks
        // If duplicate exists, merge all lot_types. For duplicate lot_type, use the one with latest update_datetime
        Map<String, List<CarParkData>> groupedByCarParkNo = new LinkedHashMap<>();
        for (CarParkData carParkData : carParkDataList) {
            String carParkNo = carParkData.getCarparkNumber();
            groupedByCarParkNo.computeIfAbsent(carParkNo, k -> new ArrayList<>()).add(carParkData);
        }

        // Merge duplicates
        List<CarParkData> mergedList = new ArrayList<>();
        int duplicateCount = 0;
        for (Map.Entry<String, List<CarParkData>> entry : groupedByCarParkNo.entrySet()) {
            List<CarParkData> carParkDataList1 = entry.getValue();
            if (carParkDataList1.size() > 1) {
                duplicateCount += carParkDataList1.size() - 1;
                log.debug("Merging {} duplicate entries for car park: {}", carParkDataList1.size(), entry.getKey());
                mergedList.add(mergeCarParkData(carParkDataList1));
            } else {
                mergedList.add(carParkDataList1.getFirst());
            }
        }

        if (duplicateCount > 0) {
            log.warn("Merged {} duplicate car park entries in API response", duplicateCount);
        }

        // Sort by carParkNo to ensure consistent ordering and prevent deadlocks
        mergedList.sort(Comparator.comparing(CarParkData::getCarparkNumber));

        // Shard the data into chunks of SHARD_SIZE records
        List<List<CarParkData>> shards = shardDataBySize(mergedList, SHARD_SIZE);
        int numShards = shards.size();

        log.info("Processing {} unique car parks (original: {}, duplicates merged: {}) using {} shards (size={} each)...",
                mergedList.size(), carParkDataList.size(), duplicateCount, numShards, SHARD_SIZE);

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalNotFound = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Process each shard in parallel
        for (int shardIndex = 0; shardIndex < shards.size(); shardIndex++) {
            final int index = shardIndex;
            List<CarParkData> shard = shards.get(index);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                log.info("Shard {} processing {} car parks...", index, shard.size());
                try {
                    ShardResult result = shardProcessor.processShard(shard, updateTime);
                    totalProcessed.addAndGet(result.processedCount);
                    totalNotFound.addAndGet(result.notFoundCount);
                    log.info("Shard {} completed: {} processed, {} not found",
                            index, result.processedCount, result.notFoundCount);
                } catch (Exception e) {
                    log.error("Error processing shard {}: {}", index, e.getMessage(), e);
                }
            }, virtualThreadExecutor);

            futures.add(future);
        }

        // Wait for all shards to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Completed processing {} records across {} shards. {} car parks not found in database.",
                totalProcessed.get(), numShards, totalNotFound.get());

        return totalProcessed.get();
    }

    /**
     * Merge duplicate CarParkData entries by combining all lot types.
     * If duplicate lot_type exists, use the one from the record with latest update_datetime.
     */
    private CarParkData mergeCarParkData(List<CarParkData> duplicates) {
        // Determine car park number (they are all the same) and compute latest infos without sorting
        String carParkNo = duplicates.isEmpty() ? null : duplicates.get(0).getCarparkNumber();

        // Track latest info per lot type along with the originating update time
        Map<String, CarParkInfo> lotTypeToInfo = new LinkedHashMap<>();
        Map<String, String> lotTypeToUpdateTime = new HashMap<>();

        String latestUpdateDatetime = null;

        for (CarParkData carParkData : duplicates) {
            String recordUpdateTimeStr = carParkData.getUpdateDatetime();
            LocalDateTime recordUpdateTime = null;
            if (recordUpdateTimeStr != null) {
                try {
                    recordUpdateTime = LocalDateTime.parse(recordUpdateTimeStr, DATE_FORMATTER);
                } catch (Exception ignored) {
                    // ignore parse errors, treat as null
                }
            }

            // Track overall latest update time
            if (latestUpdateDatetime == null || (recordUpdateTime != null &&
                    LocalDateTime.parse(latestUpdateDatetime, DATE_FORMATTER).isBefore(recordUpdateTime))) {
                latestUpdateDatetime = recordUpdateTimeStr;
            }

            if (carParkData.getCarparkInfo() == null) {
                continue;
            }

            for (CarParkInfo info : carParkData.getCarparkInfo()) {
                String lotType = info.getLotType();
                String existingTimeStr = lotTypeToUpdateTime.get(lotType);
                LocalDateTime existingTime = null;
                if (existingTimeStr != null) {
                    try {
                        existingTime = LocalDateTime.parse(existingTimeStr, DATE_FORMATTER);
                    } catch (Exception ignored) {
                    }
                }

                // Replace if there is no existing, or current record is newer
                boolean shouldReplace = false;
                if (existingTime == null) {
                    shouldReplace = true; // nothing stored yet
                } else if (recordUpdateTime != null && (existingTime == null || recordUpdateTime.isAfter(existingTime))) {
                    shouldReplace = true;
                }

                if (shouldReplace) {
                    lotTypeToInfo.put(lotType, info);
                    lotTypeToUpdateTime.put(lotType, recordUpdateTimeStr);
                }
            }
        }

        CarParkData merged = new CarParkData();
        merged.setCarparkNumber(carParkNo);
        merged.setCarparkInfo(new ArrayList<>(lotTypeToInfo.values()));
        merged.setUpdateDatetime(latestUpdateDatetime);

        return merged;
    }

    /**
     * Shard data into chunks of specified size.
     */
    private List<List<CarParkData>> shardDataBySize(List<CarParkData> data, int shardSize) {
        List<List<CarParkData>> shards = new ArrayList<>();

        for (int i = 0; i < data.size(); i += shardSize) {
            int end = Math.min(i + shardSize, data.size());
            shards.add(data.subList(i, end));
        }

        return shards;
    }
}
