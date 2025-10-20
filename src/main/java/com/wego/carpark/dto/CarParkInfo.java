package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents car park availability information for a specific lot type.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CarParkInfo(
    @JsonProperty("lot_type") String lotType,
    @JsonProperty("total_lots") int totalLots,
    @JsonProperty("lots_available") int lotsAvailable
) {}