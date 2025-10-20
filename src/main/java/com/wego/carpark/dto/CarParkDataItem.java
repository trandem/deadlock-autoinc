package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents a single item in the API response containing timestamp and car park data.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarParkDataItem {
    private String timestamp;

    @JsonProperty("carpark_data")
    private List<CarParkData> carparkData;
}