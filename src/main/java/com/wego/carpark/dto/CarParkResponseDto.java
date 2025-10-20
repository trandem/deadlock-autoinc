package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO for car park API response.
 * Represents a car park with its location and availability information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarParkResponseDto {

    @JsonProperty("address")
    private String address;

    @JsonProperty("latitude")
    private BigDecimal latitude;

    @JsonProperty("longitude")
    private BigDecimal longitude;

    @JsonProperty("total_lots")
    private Integer totalLots;

    @JsonProperty("available_lots")
    private Integer availableLots;
}
