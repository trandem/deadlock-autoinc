package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents car park data with car park number and availability info.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CarParkData(
    @JsonProperty("carpark_number") String carparkNumber,
    @JsonProperty("carpark_info") List<CarParkInfo> carparkInfo,
    @JsonProperty("update_datetime") String updateDatetime
) {}