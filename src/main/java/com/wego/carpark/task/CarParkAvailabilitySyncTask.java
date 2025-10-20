package com.wego.carpark.task;

import com.wego.carpark.dto.CarParkAvailabilityApiResponse;
import com.wego.carpark.dto.CarParkData;
import com.wego.carpark.dto.CarParkDataItem;
import com.wego.carpark.service.CarParkAvailabilityShardProcessor;
import com.wego.carpark.service.CarParkDataFetcher;
import com.wego.carpark.service.CarParkDataMerger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Task to sync car park availability from the external API.
 */
@Component
@Slf4j
public class CarParkAvailabilitySyncTask {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final int SHARD_SIZE = 200;

    private final CarParkAvailabilityShardProcessor shardProcessor;
    private final CarParkDataFetcher dataFetcher;
    private final CarParkDataMerger dataMerger;
    private final ExecutorService virtualThreadExecutor;

    public CarParkAvailabilitySyncTask(
            CarParkAvailabilityShardProcessor shardProcessor,
            CarParkDataFetcher dataFetcher,
            CarParkDataMerger dataMerger,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.shardProcessor = shardProcessor;
        this.dataFetcher = dataFetcher;
        this.dataMerger = dataMerger;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    public void syncAvailabilityData() {
        log.info("Starting car park availability sync...");

        try {
            CarParkAvailabilityApiResponse apiResponse = dataFetcher.fetchAvailabilityData();
            int updatedCount = processAvailabilityData(apiResponse);
            log.info("Car park availability sync completed. Updated {} records.", updatedCount);
        } catch (Exception e) {
            log.error("Error syncing car park availability data", e);
            throw new RuntimeException("Failed to sync availability data", e);
        }
    }

    private int processAvailabilityData(CarParkAvailabilityApiResponse apiResponse) {

        if (apiResponse.items() == null || apiResponse.items().isEmpty()) {
            log.warn("No data items found in API response");
            return 0;
        }

        CarParkDataItem firstItem = apiResponse.items().get(0);
        LocalDateTime updateTime = LocalDateTime.parse(firstItem.timestamp(), DATE_FORMATTER);

        List<CarParkData> carParkDataList = firstItem.carparkData();
        if (carParkDataList == null || carParkDataList.isEmpty()) {
            log.warn("No carpark_data found in API response");
            return 0;
        }

        int originalCount = carParkDataList.size();
        List<CarParkData> deduplicatedList = dataMerger.deduplicateAndMerge(carParkDataList);

        List<List<CarParkData>> shards = shardDataBySize(deduplicatedList);
        int numShards = shards.size();

        log.info("Processing {} unique car parks (original: {}) using {} shards (size={} each)...",
                deduplicatedList.size(), originalCount, numShards, SHARD_SIZE);

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalNotFound = new AtomicInteger(0);

        // Process each shard in parallel using IntStream
        var futures = IntStream.range(0, shards.size())
                .mapToObj(index -> {
                    var shard = shards.get(index);
                    return CompletableFuture.runAsync(() -> {
                        log.info("Shard {} processing {} car parks...", index, shard.size());
                        try {
                            var result = shardProcessor.processShard(shard, updateTime);
                            totalProcessed.addAndGet(result.processedCount());
                            log.info("Shard {} completed: {} processed",
                                    index, result.processedCount());
                        } catch (Exception e) {
                            log.error("Error processing shard {}: {}", index, e.getMessage(), e);
                        }
                    }, virtualThreadExecutor);
                })
                .toList();

        // Wait for all shards to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Completed processing {} records across {} shards. {} car parks not found in database.",
                totalProcessed.get(), numShards, totalNotFound.get());

        return totalProcessed.get();
    }

    private List<List<CarParkData>> shardDataBySize(List<CarParkData> data) {
        List<List<CarParkData>> shards = new ArrayList<>();
        for (int i = 0; i < data.size(); i += SHARD_SIZE) {
            int end = Math.min(i + SHARD_SIZE, data.size());
            shards.add(data.subList(i, end));
        }
        return shards;
    }
}
