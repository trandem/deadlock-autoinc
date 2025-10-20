package com.wego.carpark.controller;

import com.wego.carpark.dto.CarParkResponseDto;
import com.wego.carpark.dto.ErrorResponse;
import com.wego.carpark.service.CarParkService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for car park endpoints.
 * Provides API to find nearest car parks with availability.
 */
@RestController
@RequestMapping("/carparks")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CarParkController {

    private final CarParkService carParkService;

    /**
     * Get nearest car parks with available lots.
     *
     * @param latitude  User's latitude (required, range: -90 to 90)
     * @param longitude User's longitude (required, range: -180 to 180)
     * @param page      Page number (optional, default: 1, min: 1)
     * @param perPage   Results per page (optional, default: 10, min: 1, max: 100)
     * @return List of car parks sorted by distance
     */
    @GetMapping("/nearest")
    public ResponseEntity<List<CarParkResponseDto>> getNearestCarParks(
            @RequestParam(name = "latitude", required = true)
            @NotNull(message = "Latitude is required")
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            Double latitude,

            @RequestParam(name = "longitude", required = true)
            @NotNull(message = "Longitude is required")
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            Double longitude,

            @RequestParam(name = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "Page must be at least 1")
            Integer page,

            @RequestParam(name = "per_page", required = false, defaultValue = "10")
            @Min(value = 1, message = "Per page must be at least 1")
            @Max(value = 100, message = "Per page cannot exceed 100")
            Integer perPage) {

        log.info("GET /carparks/nearest - lat: {}, lon: {}, page: {}, perPage: {}",
                latitude, longitude, page, perPage);

        List<CarParkResponseDto> carParks = carParkService.findNearestCarParks(
                latitude, longitude, page, perPage);

        return ResponseEntity.ok(carParks);
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            jakarta.validation.ConstraintViolationException ex) {

        log.error("Validation error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Invalid request parameters: " + ex.getMessage())
                .path("/carparks/nearest")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Exception handler for missing required parameters.
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {

        log.error("Missing parameter: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Required parameter '" + ex.getParameterName() + "' is missing")
                .path("/carparks/nearest")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Exception handler for type mismatch errors.
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {

        log.error("Type mismatch error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Invalid parameter '" + ex.getName() + "': " + ex.getMessage())
                .path("/carparks/nearest")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Generic exception handler.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path("/carparks/nearest")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
