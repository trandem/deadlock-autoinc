package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents car park data with car park number and availability info.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarParkData {
    @JsonProperty("carpark_number")
    private String carparkNumber;

    @JsonProperty("carpark_info")
    private List<CarParkInfo> carparkInfo;

    @JsonProperty("update_datetime")
    private String updateDatetime;
}