package com.wego.carpark.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CoordinateConverter.
 * Tests conversion of SVY21 coordinates to WGS84 (GPS) coordinates.
 */
@DisplayName("CoordinateConverter Tests")
class CoordinateConverterTest {

    @Test
    @DisplayName("Should convert SVY21 to WGS84 with double values")
    void shouldConvertSvy21ToWgs84WithDoubleValues() {
        // Given: Known SVY21 coordinates (example from Singapore)
        double x = 30000.0; // Easting
        double y = 40000.0; // Northing

        // When: Converting to WGS84
        var result = CoordinateConverter.svy21ToWgs84(x, y);

        // Then: Should return valid latitude and longitude
        assertNotNull(result);
        assertEquals(2, result.length);

        // Verify latitude is within Singapore's bounds (approximately 1.15 to 1.47)
        assertTrue(result[0] > 1.0 && result[0] < 2.0,
            "Latitude should be within Singapore's range");

        // Verify longitude is within Singapore's bounds (approximately 103.6 to 104.0)
        assertTrue(result[1] > 103.0 && result[1] < 105.0,
            "Longitude should be within Singapore's range");
    }

    @Test
    @DisplayName("Should convert SVY21 to WGS84 with BigDecimal values")
    void shouldConvertSvy21ToWgs84WithBigDecimalValues() {
        // Given: Known SVY21 coordinates as BigDecimal
        var x = new BigDecimal("30000.0");
        var y = new BigDecimal("40000.0");

        // When: Converting to WGS84
        var result = CoordinateConverter.svy21ToWgs84(x, y);

        // Then: Should return valid latitude and longitude as BigDecimal
        assertNotNull(result);
        assertEquals(2, result.length);
        assertInstanceOf(BigDecimal.class, result[0]);
        assertInstanceOf(BigDecimal.class, result[1]);

        // Verify values are within Singapore's bounds
        assertTrue(result[0].doubleValue() > 1.0 && result[0].doubleValue() < 2.0);
        assertTrue(result[1].doubleValue() > 103.0 && result[1].doubleValue() < 105.0);
    }

    @Test
    @DisplayName("Should handle zero coordinates")
    void shouldHandleZeroCoordinates() {
        // Given: Zero coordinates
        double x = 0.0;
        double y = 0.0;

        // When: Converting to WGS84
        var result = CoordinateConverter.svy21ToWgs84(x, y);

        // Then: Should return valid result (may be outside Singapore)
        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    @DisplayName("Should handle negative coordinates")
    void shouldHandleNegativeCoordinates() {
        // Given: Negative coordinates
        double x = -10000.0;
        double y = -10000.0;

        // When: Converting to WGS84
        var result = CoordinateConverter.svy21ToWgs84(x, y);

        // Then: Should return valid result
        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    @DisplayName("Should produce consistent results for same input")
    void shouldProduceConsistentResults() {
        // Given: Same coordinates
        double x = 28983.788;
        double y = 38309.382;

        // When: Converting multiple times
        var result1 = CoordinateConverter.svy21ToWgs84(x, y);
        var result2 = CoordinateConverter.svy21ToWgs84(x, y);

        // Then: Results should be identical
        assertArrayEquals(result1, result2);
    }

    @Test
    @DisplayName("Should handle large coordinate values")
    void shouldHandleLargeCoordinateValues() {
        // Given: Large SVY21 coordinates
        double x = 50000.0;
        double y = 50000.0;

        // When: Converting to WGS84
        var result = CoordinateConverter.svy21ToWgs84(x, y);

        // Then: Should return valid result
        assertNotNull(result);
        assertEquals(2, result.length);
        assertFalse(Double.isNaN(result[0]));
        assertFalse(Double.isNaN(result[1]));
        assertFalse(Double.isInfinite(result[0]));
        assertFalse(Double.isInfinite(result[1]));
    }

    @Test
    @DisplayName("Should convert typical HDB car park coordinates correctly")
    void shouldConvertTypicalHdbCarParkCoordinates() {
        // Given: Typical HDB car park SVY21 coordinates
        // Example: Bishan area
        double x = 29050.0;
        double y = 38900.0;

        // When: Converting to WGS84
        var result = CoordinateConverter.svy21ToWgs84(x, y);

        // Then: Should return coordinates within Singapore's main island
        assertNotNull(result);
        assertTrue(result[0] > 1.2 && result[0] < 1.5,
            "Latitude should be within Singapore's main island");
        assertTrue(result[1] > 103.6 && result[1] < 104.0,
            "Longitude should be within Singapore's main island");
    }
}