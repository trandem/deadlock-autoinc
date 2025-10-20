package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Root response from car park availability API.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarParkAvailabilityApiResponse {
    private List<CarParkDataItem> items;
}