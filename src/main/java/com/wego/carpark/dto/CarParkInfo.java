package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents car park availability information for a specific lot type.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarParkInfo {
    @JsonProperty("lot_type")
    private String lotType;

    @JsonProperty("total_lots")
    private int totalLots;

    @JsonProperty("lots_available")
    private int lotsAvailable;
}