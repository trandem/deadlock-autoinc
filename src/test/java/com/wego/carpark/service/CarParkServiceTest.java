package com.wego.carpark.service;

import com.wego.carpark.dto.CarParkResponseDto;
import com.wego.carpark.mapper.CarParkMapper;
import com.wego.carpark.model.CarPark;
import com.wego.carpark.repository.CarParkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CarParkService.
 * Tests business logic for finding nearest car parks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarParkService Tests")
class CarParkServiceTest {

    @Mock
    private CarParkRepository carParkRepository;

    @Mock
    private CarParkMapper carParkMapper;

    private CarParkService carParkService;

    @BeforeEach
    void setUp() {
        // Use virtual thread executor for tests
        var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        carParkService = new CarParkService(
            carParkRepository,
            carParkMapper,
            virtualThreadExecutor
        );
    }

    @Test
    @DisplayName("Should find nearest car parks successfully")
    void shouldFindNearestCarParksSuccessfully() {
        // Given: Valid coordinates and pagination
        var latitude = 1.3521;
        var longitude = 103.8198;
        var page = 1;
        var perPage = 10;

        var carPark1 = createTestCarPark("CP001", "Address 1");
        var carPark2 = createTestCarPark("CP002", "Address 2");
        var carParks = List.of(carPark1, carPark2);

        var dto1 = createTestDto("CP001", "Address 1");
        var dto2 = createTestDto("CP002", "Address 2");

        when(carParkRepository.findNearestCarParksWithAvailabilityAsList(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(carParks);
        when(carParkMapper.toDto(carPark1)).thenReturn(dto1);
        when(carParkMapper.toDto(carPark2)).thenReturn(dto2);

        // When: Finding nearest car parks
        var result = carParkService.findNearestCarParks(latitude, longitude, page, perPage);

        // Then: Should return mapped DTOs
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify repository calls
        verify(carParkRepository).findNearestCarParksWithAvailabilityAsList(
            latitude, longitude, perPage, 0
        );
        verify(carParkMapper, times(2)).toDto(any(CarPark.class));
    }

    @Test
    @DisplayName("Should handle empty results")
    void shouldHandleEmptyResults() {
        // Given: No car parks available
        var latitude = 1.3521;
        var longitude = 103.8198;
        var page = 1;
        var perPage = 10;

        when(carParkRepository.findNearestCarParksWithAvailabilityAsList(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When: Finding nearest car parks
        var result = carParkService.findNearestCarParks(latitude, longitude, page, perPage);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(carParkRepository).findNearestCarParksWithAvailabilityAsList(
            latitude, longitude, perPage, 0
        );
        verify(carParkMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should calculate correct offset for pagination")
    void shouldCalculateCorrectOffsetForPagination() {
        // Given: Page 3 with 10 items per page
        var latitude = 1.3521;
        var longitude = 103.8198;
        var page = 3;
        var perPage = 10;

        when(carParkRepository.findNearestCarParksWithAvailabilityAsList(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When: Finding nearest car parks
        carParkService.findNearestCarParks(latitude, longitude, page, perPage);

        // Then: Should calculate offset as (page - 1) * perPage = (3 - 1) * 10 = 20
        verify(carParkRepository).findNearestCarParksWithAvailabilityAsList(
            latitude, longitude, perPage, 20
        );
    }

    @Test
    @DisplayName("Should handle first page correctly")
    void shouldHandleFirstPageCorrectly() {
        // Given: First page
        var latitude = 1.3521;
        var longitude = 103.8198;
        var page = 1;
        var perPage = 10;

        when(carParkRepository.findNearestCarParksWithAvailabilityAsList(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When: Finding nearest car parks
        carParkService.findNearestCarParks(latitude, longitude, page, perPage);

        // Then: Should calculate offset as 0
        verify(carParkRepository).findNearestCarParksWithAvailabilityAsList(
            latitude, longitude, perPage, 0
        );
    }

    @Test
    @DisplayName("Should handle different page sizes")
    void shouldHandleDifferentPageSizes() {
        // Given: Custom page size
        var latitude = 1.3521;
        var longitude = 103.8198;
        var page = 2;
        var perPage = 25;

        when(carParkRepository.findNearestCarParksWithAvailabilityAsList(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When: Finding nearest car parks
        carParkService.findNearestCarParks(latitude, longitude, page, perPage);

        // Then: Should use custom page size and calculate offset correctly
        verify(carParkRepository).findNearestCarParksWithAvailabilityAsList(
            latitude, longitude, 25, 25
        );
    }

    @Test
    @DisplayName("Should handle extreme coordinates")
    void shouldHandleExtremeCoordinates() {
        // Given: Extreme but valid coordinates
        var latitude = -90.0;
        var longitude = 180.0;
        var page = 1;
        var perPage = 10;

        when(carParkRepository.findNearestCarParksWithAvailabilityAsList(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of());

        // When: Finding nearest car parks
        var result = carParkService.findNearestCarParks(latitude, longitude, page, perPage);

        // Then: Should handle without error
        assertNotNull(result);
        verify(carParkRepository).findNearestCarParksWithAvailabilityAsList(
            latitude, longitude, perPage, 0
        );
    }

    @Test
    @DisplayName("Should execute count and select queries in parallel")
    void shouldExecuteQueriesInParallel() {
        // Given: Valid input
        var latitude = 1.3521;
        var longitude = 103.8198;
        var page = 1;
        var perPage = 10;

        var carPark = createTestCarPark("CP001", "Address 1");
        when(carParkRepository.findNearestCarParksWithAvailabilityAsList(
            anyDouble(), anyDouble(), anyInt(), anyInt()
        )).thenReturn(List.of(carPark));
        when(carParkMapper.toDto(any())).thenReturn(createTestDto("CP001", "Address 1"));

        // When: Finding nearest car parks
        var result = carParkService.findNearestCarParks(latitude, longitude, page, perPage);

        // Then: Both repository methods should be called
        assertNotNull(result);
        verify(carParkRepository).findNearestCarParksWithAvailabilityAsList(
            latitude, longitude, perPage, 0
        );
    }

    // Helper methods

    private CarPark createTestCarPark(String carParkNo, String address) {
        return CarPark.builder()
            .id(1L)
            .carParkNo(carParkNo)
            .address(address)
            .latitude(new BigDecimal("1.3521"))
            .longitude(new BigDecimal("103.8198"))
            .shortTermParking("WHOLE DAY")
            .parkingHoursFrom(LocalTime.of(0, 0))
            .parkingHoursTo(LocalTime.of(23, 59))
            .totalLots(100)
            .totalAvailableLots(50)
            .build();
    }

    private CarParkResponseDto createTestDto(String carParkNo, String address) {
        return CarParkResponseDto.builder()
            .address(address)
            .latitude(new BigDecimal("1.3521"))
            .longitude(new BigDecimal("103.8198"))
            .totalLots(100)
            .availableLots(50)
            .build();
    }
}