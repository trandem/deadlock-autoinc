package com.wego.carpark.repository;

import com.wego.carpark.model.CarParkAvailability;
import com.wego.carpark.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * Custom repository implementation for batch operations on CarParkAvailability.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CarParkAvailabilityRepositoryImpl implements CarParkAvailabilityRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public void batchUpsertAvailabilities(List<CarParkAvailability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return;
        }

        // Generate Snowflake IDs for records without IDs
        for (var availability : availabilities) {
            if (availability.getId() == null) {
                availability.setId(idGenerator.nextId());
            }
        }

        var sql = """
            INSERT INTO car_park_availability
                (id, car_park_id, car_park_no, lot_type, total_lots, available_lots, update_datetime, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                total_lots = VALUES(total_lots),
                available_lots = VALUES(available_lots),
                update_datetime = VALUES(update_datetime)
            """;

        jdbcTemplate.batchUpdate(sql, availabilities, availabilities.size(),
            (ps, availability) -> {
                ps.setLong(1, availability.getId());
                ps.setLong(2, availability.getCarParkId());
                ps.setString(3, availability.getCarParkNo());
                ps.setString(4, availability.getLotType());
                ps.setInt(5, availability.getTotalLots());
                ps.setInt(6, availability.getAvailableLots());
                ps.setTimestamp(7, Timestamp.valueOf(availability.getUpdateDatetime()));
            });

        log.debug("Batch upserted {} car park availabilities", availabilities.size());
    }
}