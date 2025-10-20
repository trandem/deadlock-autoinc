package com.wego.carpark.task;

import com.wego.carpark.model.CarPark;
import com.wego.carpark.repository.CarParkRepository;
import com.wego.carpark.util.CoordinateConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Task to import car park information from CSV file.
 * This runs on application startup to populate the database with car park data.
 * Coordinates are converted from SVY21 to WGS84 format.
 */
@Component
@Order(1)
@Slf4j
public class CarParkDataImportTask implements CommandLineRunner {

    private final CarParkRepository carParkRepository;
    private static final String CSV_FILE = "HDBCarparkInformation.csv";
    private static final int BATCH_SIZE = 500;

    public CarParkDataImportTask(CarParkRepository carParkRepository) {
        this.carParkRepository = carParkRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting car park data import task...");

        // Check if data already exists
        long existingCount = carParkRepository.count();
        if (existingCount > 0) {
            log.info("Car park data already exists ({} records). Skipping import.", existingCount);
            return;
        }

        try {
            importCarParkData();
            log.info("Car park data import completed successfully");
        } catch (Exception e) {
            log.error("Error importing car park data", e);
            // Don't throw exception to allow application to start
        }
    }

    /**
     * Import car park data from CSV file.
     * Processes records sequentially and saves in batches for better performance.
     */
    private void importCarParkData() throws Exception {
        ClassPathResource resource = new ClassPathResource(CSV_FILE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            List<CarPark> carParks = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;
            int totalRecords = 0;

            log.info("Processing CSV records...");

            // Process each record sequentially
            for (CSVRecord record : csvParser) {
                totalRecords++;
                try {
                    CarPark carPark = parseCarParkRecord(record);
                    carParks.add(carPark);
                    successCount++;

                    // Log progress every 100 records
                    if (successCount % 100 == 0) {
                        log.info("Parsed {} car parks...", successCount);
                    }
                } catch (Exception e) {
                    errorCount++;
                    try {
                        log.warn("Error parsing record {}: {}", record.get("car_park_no"), e.getMessage());
                    } catch (Exception ex) {
                        log.warn("Error parsing record: {}", e.getMessage());
                    }
                }
            }

            // Save any remaining records
            if (!carParks.isEmpty()) {
                carParkRepository.saveAll(carParks);
                log.info("Saved final batch of {} car parks (total: {})", carParks.size(), successCount);
            }

            log.info("Import completed: {} success, {} errors out of {} total records",
                    successCount, errorCount, totalRecords);
        }
    }

    private CarPark parseCarParkRecord(CSVRecord record) {
        String carParkNo = record.get("car_park_no");
        String address = record.get("address");

        // Read SVY21 coordinates from CSV for conversion (not stored in database)
        // x_coord (easting) maps to longitude, y_coord (northing) maps to latitude
        BigDecimal xCoord = parseBigDecimal(record.get("x_coord")); // SVY21 Easting
        BigDecimal yCoord = parseBigDecimal(record.get("y_coord")); // SVY21 Northing

        // Convert SVY21 to WGS84 (GPS coordinates)
        // Converter returns [latitude, longitude]
        BigDecimal[] wgs84 = CoordinateConverter.svy21ToWgs84(xCoord, yCoord);
        BigDecimal latitude = wgs84[0];   // From y_coord (northing)
        BigDecimal longitude = wgs84[1];  // From x_coord (easting)

        // Parse parking hours from short_term_parking column
        String shortTermParking = getStringOrNull(record, "short_term_parking");
        LocalTime[] parkingHours = parseTimeRange(shortTermParking);

        return CarPark.builder()
                .carParkNo(carParkNo)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .carParkType(getStringOrNull(record, "car_park_type"))
                .typeOfParkingSystem(getStringOrNull(record, "type_of_parking_system"))
                .shortTermParking(shortTermParking)
                .parkingHoursFrom(parkingHours[0])
                .parkingHoursTo(parkingHours[1])
                .freeParking(getStringOrNull(record, "free_parking"))
                .nightParking(getStringOrNull(record, "night_parking"))
                .carParkDecks(parseInteger(record.get("car_park_decks")))
                .gantryHeight(parseBigDecimal(record.get("gantry_height")))
                .carParkBasement(getStringOrNull(record, "car_park_basement"))
                .totalLots(parseIntegerSafe(record, "total_lots"))
                .totalAvailableLots(parseIntegerSafe(record, "total_available_lots"))
                .build();
    }

    /**
     * Parse time range from short_term_parking column.
     * Handles formats like: "7AM-10.30PM", "7AM-7PM", "WHOLE DAY", "NO"
     * Returns array [from, to] where both can be null.
     */
    private LocalTime[] parseTimeRange(String value) {
        LocalTime[] result = new LocalTime[2];

        if (value == null || value.trim().isEmpty()) {
            return result;
        }

        value = value.trim().toUpperCase();

        // Handle special cases
        if (value.equals("WHOLE DAY")) {
            // WHOLE DAY can be represented as full day hours
            result[0] = LocalTime.of(0, 0);
            result[1] = LocalTime.of(23, 59);
            return result;
        }

        if (value.equals("NO") || value.equals("N/A")) {
            // No parking hours available
            return result;
        }

        // Handle time ranges like "7AM-10.30PM" or "7AM-7PM"
        if (value.contains("-")) {
            try {
                String[] parts = value.split("-");
                if (parts.length == 2) {
                    result[0] = parseTime(parts[0].trim());
                    result[1] = parseTime(parts[1].trim());
                }
            } catch (Exception e) {
                log.warn("Failed to parse time range: {}", value);
            }
        }

        return result;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getStringOrNull(CSVRecord record, String column) {
        String value = record.get(column);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /**
     * Parse integer value from CSV record with safe handling for missing columns.
     * Returns null if column doesn't exist or value is empty/invalid.
     */
    private Integer parseIntegerSafe(CSVRecord record, String column) {
        try {
            String value = record.get(column);
            return parseInteger(value);
        } catch (IllegalArgumentException e) {
            // Column doesn't exist in CSV
            return null;
        }
    }

    /**
     * Parse time value from CSV record with safe handling for missing columns.
     * Supports formats like: 7AM, 10.30PM, 07:00, 22:30, WHOLE DAY, NO
     * Returns null if column doesn't exist, value is empty/invalid, or represents special cases.
     */
    private LocalTime parseTimeSafe(CSVRecord record, String column) {
        try {
            String value = record.get(column);
            return parseTime(value);
        } catch (IllegalArgumentException e) {
            // Column doesn't exist in CSV
            return null;
        }
    }

    /**
     * Parse time string into LocalTime.
     * Handles various formats: 7AM, 10.30PM, 07:00, 22:30
     * Returns null for special values like WHOLE DAY, NO, or empty strings.
     */
    private LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim().toUpperCase();

        // Handle special cases
        if (value.equals("WHOLE DAY") || value.equals("NO") || value.equals("N/A")) {
            return null;
        }

        try {
            // Try parsing formats like "7AM", "10.30PM"
            if (value.contains("AM") || value.contains("PM")) {
                return parseTimeAmPm(value);
            }

            // Try parsing ISO format (HH:mm or HH:mm:ss)
            if (value.contains(":")) {
                return LocalTime.parse(value);
            }

            return null;
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse time value: {}", value);
            return null;
        }
    }

    /**
     * Parse time in AM/PM format (e.g., "7AM", "10.30PM")
     */
    private LocalTime parseTimeAmPm(String value) {
        boolean isPM = value.endsWith("PM");
        String timeStr = value.replace("AM", "").replace("PM", "").trim();

        // Parse hours and minutes
        int hours;
        int minutes = 0;

        if (timeStr.contains(".")) {
            String[] parts = timeStr.split("\\.");
            hours = Integer.parseInt(parts[0]);
            if (parts.length > 1) {
                minutes = Integer.parseInt(parts[1]);
            }
        } else {
            hours = Integer.parseInt(timeStr);
        }

        // Convert to 24-hour format
        if (isPM && hours != 12) {
            hours += 12;
        } else if (!isPM && hours == 12) {
            hours = 0;
        }

        return LocalTime.of(hours, minutes);
    }
}
