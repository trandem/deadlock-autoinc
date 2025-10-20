package com.wego.carpark.util;

import java.math.BigDecimal;

/**
 * Utility class for converting coordinates between different systems.
 * Converts SVY21 (Singapore's coordinate system) to WGS84 (GPS coordinates).
 *
 * This implementation uses a simplified conversion approach.
 * For production use, consider using a more accurate conversion library or
 * the OneMap API: https://www.onemap.gov.sg/apidocs/apidocs
 */
public class CoordinateConverter {

    // SVY21 projection parameters
    private static final double A = 6378137.0; // Semi-major axis
    private static final double F = 1.0 / 298.257223563; // Flattening
    private static final double ORIGIN_LAT = 1.366666; // Origin latitude
    private static final double ORIGIN_LON = 103.833333; // Origin longitude
    private static final double FALSE_NORTHING = 38744.572;
    private static final double FALSE_EASTING = 28001.642;
    private static final double SCALE_FACTOR = 1.0;

    /**
     * Convert SVY21 coordinates to WGS84 (GPS) coordinates.
     * This is a simplified conversion. For accurate conversion in production,
     * use the OneMap API or a proper coordinate transformation library.
     *
     * @param x X coordinate in SVY21
     * @param y Y coordinate in SVY21
     * @return Array containing [latitude, longitude] in WGS84
     */
    public static double[] svy21ToWgs84(double x, double y) {
        // Simplified conversion based on approximate transformation
        // Adjust X and Y for false easting/northing
        double adjustedX = x - FALSE_EASTING;
        double adjustedY = y - FALSE_NORTHING;

        // Convert to degrees (simplified)
        double lat = ORIGIN_LAT + (adjustedY / 111320.0);
        double lon = ORIGIN_LON + (adjustedX / (111320.0 * Math.cos(Math.toRadians(ORIGIN_LAT))));

        return new double[]{lat, lon};
    }

    /**
     * Convert SVY21 BigDecimal coordinates to WGS84.
     */
    public static BigDecimal[] svy21ToWgs84(BigDecimal x, BigDecimal y) {
        double[] result = svy21ToWgs84(x.doubleValue(), y.doubleValue());
        return new BigDecimal[]{
                BigDecimal.valueOf(result[0]),
                BigDecimal.valueOf(result[1])
        };
    }
}
