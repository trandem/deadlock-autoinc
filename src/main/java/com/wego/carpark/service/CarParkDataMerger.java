package com.wego.carpark.service;

import com.wego.carpark.dto.CarParkData;
import com.wego.carpark.dto.CarParkInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for merging duplicate car park data entries.
 * Handles deduplication to prevent database deadlocks.
 */
@Service
@Slf4j
public class CarParkDataMerger {

    public List<CarParkData> deduplicateAndMerge(List<CarParkData> carParkDataList) {
        Map<String, List<CarParkData>> groupedByCarParkNo = groupByCarParkNo(carParkDataList);
        List<CarParkData> mergedList = mergeGroupedData(groupedByCarParkNo);
        mergedList.sort(Comparator.comparing(CarParkData::carparkNumber));
        return mergedList;
    }

    private Map<String, List<CarParkData>> groupByCarParkNo(List<CarParkData> carParkDataList) {
        Map<String, List<CarParkData>> grouped = new LinkedHashMap<>();
        for (CarParkData carParkData : carParkDataList) {
            String carParkNo = carParkData.carparkNumber();
            grouped.computeIfAbsent(carParkNo, k -> new ArrayList<>()).add(carParkData);
        }
        return grouped;
    }

    private List<CarParkData> mergeGroupedData(Map<String, List<CarParkData>> groupedByCarParkNo) {
        List<CarParkData> mergedList = new ArrayList<>();
        int duplicateCount = 0;

        for (Map.Entry<String, List<CarParkData>> entry : groupedByCarParkNo.entrySet()) {
            List<CarParkData> dataList = entry.getValue();
            if (dataList.size() > 1) {
                duplicateCount += dataList.size() - 1;
                log.debug("Merging {} duplicate entries for car park: {}", dataList.size(), entry.getKey());
                mergedList.add(mergeCarParkData(dataList));
            } else {
                mergedList.add(dataList.get(0));
            }
        }

        if (duplicateCount > 0) {
            log.warn("Merged {} duplicate car park entries in API response", duplicateCount);
        }

        return mergedList;
    }

    private CarParkData mergeCarParkData(List<CarParkData> duplicates) {
        duplicates.sort(this::compareByUpdateDatetime);

        String carParkNo = duplicates.get(0).carparkNumber();
        Map<String, CarParkInfo> mergedInfo = mergeLotTypes(duplicates);

        return new CarParkData(
            carParkNo,
            new ArrayList<>(mergedInfo.values()),
            duplicates.get(duplicates.size() - 1).updateDatetime()
        );
    }

    private int compareByUpdateDatetime(CarParkData a, CarParkData b) {
        if (a.updateDatetime() == null && b.updateDatetime() == null) return 0;
        if (a.updateDatetime() == null) return -1;
        if (b.updateDatetime() == null) return 1;
        return a.updateDatetime().compareTo(b.updateDatetime());
    }

    private Map<String, CarParkInfo> mergeLotTypes(List<CarParkData> duplicates) {
        Map<String, CarParkInfo> mergedInfo = new LinkedHashMap<>();
        for (CarParkData carParkData : duplicates) {
            if (carParkData.carparkInfo() != null) {
                for (CarParkInfo info : carParkData.carparkInfo()) {
                    mergedInfo.put(info.lotType(), info);
                }
            }
        }
        return mergedInfo;
    }
}
