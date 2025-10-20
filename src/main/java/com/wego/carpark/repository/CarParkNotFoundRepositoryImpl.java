package com.wego.carpark.repository;

import com.wego.carpark.model.CarParkNotFound;
import com.wego.carpark.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * Custom repository implementation for batch operations on CarParkNotFound.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class CarParkNotFoundRepositoryImpl implements CarParkNotFoundRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public void batchUpsertNotFound(List<CarParkNotFound> notFoundRecords) {
        if (notFoundRecords == null || notFoundRecords.isEmpty()) {
            return;
        }

        // Generate Snowflake IDs for records without IDs
        for (var record : notFoundRecords) {
            if (record.getId() == null) {
                record.setId(idGenerator.nextId());
            }
        }

        var sql = """
            INSERT INTO car_park_not_found
                (id, car_park_no, first_seen_at, last_seen_at, occurrence_count)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                last_seen_at = VALUES(last_seen_at),
                occurrence_count = occurrence_count + 1
            """;

        jdbcTemplate.batchUpdate(sql, notFoundRecords, notFoundRecords.size(),
            (ps, record) -> {
                ps.setLong(1, record.getId());
                ps.setString(2, record.getCarParkNo());
                ps.setTimestamp(3, Timestamp.valueOf(record.getFirstSeenAt()));
                ps.setTimestamp(4, Timestamp.valueOf(record.getLastSeenAt()));
                ps.setInt(5, record.getOccurrenceCount());
            });

        log.debug("Batch upserted {} car park not found records", notFoundRecords.size());
    }
}