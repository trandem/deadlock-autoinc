package com.wego.carpark.controller;

import com.wego.carpark.dto.CarParkResponseDto;
import com.wego.carpark.service.CarParkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CarParkController.
 * Tests REST API endpoints and validation.
 */
@WebMvcTest(CarParkController.class)
@DisplayName("CarParkController Integration Tests")
class CarParkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarParkService carParkService;

    @Test
    @DisplayName("GET /carparks/nearest - Should return nearest car parks with valid parameters")
    void shouldReturnNearestCarParksWithValidParameters() throws Exception {
        // Given: Valid request parameters
        var latitude = 1.3521;
        var longitude = 103.8198;

        var carPark1 = createTestDto("CP001", "123 Test Street", 100, 50);
        var carPark2 = createTestDto("CP002", "456 Sample Road", 200, 100);
        var mockResponse = List.of(carPark1, carPark2);

        when(carParkService.findNearestCarParks(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(mockResponse);

        // When & Then: Making request should return car parks
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", String.valueOf(latitude))
                .param("longitude", String.valueOf(longitude)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].address").value("123 Test Street"))
            .andExpect(jsonPath("$[0].total_lots").value(100))
            .andExpect(jsonPath("$[0].available_lots").value(50))
            .andExpect(jsonPath("$[1].address").value("456 Sample Road"));

        verify(carParkService).findNearestCarParks(latitude, longitude, 1, 10);
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should handle custom pagination parameters")
    void shouldHandleCustomPaginationParameters() throws Exception {
        // Given: Custom pagination
        var latitude = 1.3521;
        var longitude = 103.8198;
        var page = 2;
        var perPage = 25;

        when(carParkService.findNearestCarParks(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When & Then: Should pass pagination to service
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", String.valueOf(latitude))
                .param("longitude", String.valueOf(longitude))
                .param("page", String.valueOf(page))
                .param("per_page", String.valueOf(perPage)))
            .andExpect(status().isOk());

        verify(carParkService).findNearestCarParks(latitude, longitude, page, perPage);
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should use default pagination when not specified")
    void shouldUseDefaultPaginationWhenNotSpecified() throws Exception {
        // Given: No pagination parameters
        var latitude = 1.3521;
        var longitude = 103.8198;

        when(carParkService.findNearestCarParks(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When & Then: Should use defaults (page=1, perPage=10)
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", String.valueOf(latitude))
                .param("longitude", String.valueOf(longitude)))
            .andExpect(status().isOk());

        verify(carParkService).findNearestCarParks(latitude, longitude, 1, 10);
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when latitude is missing")
    void shouldReturn400WhenLatitudeIsMissing() throws Exception {
        // When & Then: Missing latitude should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("longitude", "103.8198"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when longitude is missing")
    void shouldReturn400WhenLongitudeIsMissing() throws Exception {
        // When & Then: Missing longitude should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when latitude is out of range (too high)")
    void shouldReturn400WhenLatitudeIsTooHigh() throws Exception {
        // When & Then: Latitude > 90 should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "91.0")
                .param("longitude", "103.8198"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when latitude is out of range (too low)")
    void shouldReturn400WhenLatitudeIsTooLow() throws Exception {
        // When & Then: Latitude < -90 should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "-91.0")
                .param("longitude", "103.8198"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when longitude is out of range (too high)")
    void shouldReturn400WhenLongitudeIsTooHigh() throws Exception {
        // When & Then: Longitude > 180 should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "181.0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when longitude is out of range (too low)")
    void shouldReturn400WhenLongitudeIsTooLow() throws Exception {
        // When & Then: Longitude < -180 should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "-181.0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when page is less than 1")
    void shouldReturn400WhenPageIsLessThan1() throws Exception {
        // When & Then: Page < 1 should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "103.8198")
                .param("page", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when per_page is less than 1")
    void shouldReturn400WhenPerPageIsLessThan1() throws Exception {
        // When & Then: Per page < 1 should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "103.8198")
                .param("per_page", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 when per_page exceeds 100")
    void shouldReturn400WhenPerPageExceeds100() throws Exception {
        // When & Then: Per page > 100 should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "103.8198")
                .param("per_page", "101"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should accept maximum valid per_page value")
    void shouldAcceptMaximumValidPerPageValue() throws Exception {
        // Given: Maximum valid per_page
        when(carParkService.findNearestCarParks(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When & Then: Per page = 100 should be valid
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "103.8198")
                .param("per_page", "100"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should handle empty result set")
    void shouldHandleEmptyResultSet() throws Exception {
        // Given: Service returns empty list
        when(carParkService.findNearestCarParks(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When & Then: Should return empty array
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "103.8198"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should handle Singapore coordinates")
    void shouldHandleSingaporeCoordinates() throws Exception {
        // Given: Singapore coordinates
        var latitude = 1.3521; // Central Singapore
        var longitude = 103.8198;

        when(carParkService.findNearestCarParks(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When & Then: Should accept Singapore coordinates
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", String.valueOf(latitude))
                .param("longitude", String.valueOf(longitude)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 for invalid latitude format")
    void shouldReturn400ForInvalidLatitudeFormat() throws Exception {
        // When & Then: Invalid format should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "invalid")
                .param("longitude", "103.8198"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /carparks/nearest - Should return 400 for invalid longitude format")
    void shouldReturn400ForInvalidLongitudeFormat() throws Exception {
        // When & Then: Invalid format should return bad request
        mockMvc.perform(get("/carparks/nearest")
                .param("latitude", "1.3521")
                .param("longitude", "invalid"))
            .andExpect(status().isBadRequest());
    }

    // Helper method

    private CarParkResponseDto createTestDto(
        String carParkNo,
        String address,
        Integer totalLots,
        Integer availableLots
    ) {
        return CarParkResponseDto.builder()
            .address(address)
            .latitude(new BigDecimal("1.3521"))
            .longitude(new BigDecimal("103.8198"))
            .totalLots(totalLots)
            .availableLots(availableLots)
            .build();
    }
}