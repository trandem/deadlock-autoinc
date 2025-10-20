package com.wego.carpark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Root response from car park availability API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CarParkAvailabilityApiResponse(
    List<CarParkDataItem> items
) {}