package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a single item in the API response containing timestamp and car park data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CarParkDataItem(
    String timestamp,
    @JsonProperty("carpark_data") List<CarParkData> carparkData
) {}