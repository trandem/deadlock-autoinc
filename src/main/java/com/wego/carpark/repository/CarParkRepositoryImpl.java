package com.wego.carpark.repository;

import com.wego.carpark.model.CarPark;
import com.wego.carpark.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Custom repository implementation for batch operations on CarPark.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CarParkRepositoryImpl implements CarParkRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public void batchUpsertCarParkTotals(List<CarPark> carParks) {
        if (carParks == null || carParks.isEmpty()) {
            return;
        }

        // Generate Snowflake IDs for records without IDs
        for (var carPark : carParks) {
            if (carPark.getId() == null) {
                carPark.setId(idGenerator.nextId());
            }
        }

        var sql = """
            INSERT INTO car_parks
                (id, car_park_no, address, latitude, longitude, car_park_type, type_of_parking_system,
                 short_term_parking, parking_hours_from, parking_hours_to, free_parking, night_parking,
                 car_park_decks, gantry_height, car_park_basement, total_lots, total_available_lots,
                 created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                total_lots = VALUES(total_lots),
                total_available_lots = VALUES(total_available_lots),
                updated_at = NOW()
            """;

        jdbcTemplate.batchUpdate(sql, carParks, carParks.size(),
            (ps, carPark) -> {
                ps.setLong(1, carPark.getId());
                ps.setString(2, carPark.getCarParkNo());
                ps.setString(3, carPark.getAddress());
                ps.setBigDecimal(4, carPark.getLatitude());
                ps.setBigDecimal(5, carPark.getLongitude());
                ps.setString(6, carPark.getCarParkType());
                ps.setString(7, carPark.getTypeOfParkingSystem());
                ps.setString(8, carPark.getShortTermParking());
                ps.setObject(9, carPark.getParkingHoursFrom());
                ps.setObject(10, carPark.getParkingHoursTo());
                ps.setString(11, carPark.getFreeParking());
                ps.setString(12, carPark.getNightParking());
                ps.setObject(13, carPark.getCarParkDecks());
                ps.setBigDecimal(14, carPark.getGantryHeight());
                ps.setString(15, carPark.getCarParkBasement());
                ps.setObject(16, carPark.getTotalLots());
                ps.setObject(17, carPark.getTotalAvailableLots());
            });

        log.debug("Batch upserted {} car parks with totals", carParks.size());
    }
}