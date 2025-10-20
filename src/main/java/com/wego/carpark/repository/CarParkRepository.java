package com.wego.carpark.repository;

import com.wego.carpark.model.CarPark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CarPark entity.
 * Provides methods to query car parks with availability information.
 */
@Repository
public interface CarParkRepository extends JpaRepository<CarPark, Long> {

    /**
     * Find car park by car park number.
     */
    Optional<CarPark> findByCarParkNo(String carParkNo);

    /**
     * Find car parks by list of car park numbers (batch query).
     */
    List<CarPark> findByCarParkNoIn(List<String> carParkNos);

    /**
     * Count car parks with available lots (for parallel execution).
     * This is the count-only query that runs independently.
     *
     * @return Total count of car parks with availability
     */
    @Query(value = """
            SELECT COUNT(cp.id)
            FROM car_parks cp
            WHERE cp.total_available_lots > 0
            """,
        nativeQuery = true)
    long countCarParksWithAvailability();

    /**
     * Find car parks as a List (without count) for parallel execution.
     * Uses Haversine formula to calculate distance.
     *
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param limit Limit number of results
     * @param offset Offset for pagination
     * @return List of car parks sorted by distance
     */
    @Query(value = """
        SELECT cp.*
        FROM car_parks cp
        WHERE cp.total_available_lots > 0
        ORDER BY (6371 * acos(cos(radians(:latitude))
                 * cos(radians(cp.latitude))
                 * cos(radians(cp.longitude) - radians(:longitude))
                 + sin(radians(:latitude))
                 * sin(radians(cp.latitude)))) ASC, id desc 
        LIMIT :limit OFFSET :offset
        """,
        nativeQuery = true)
    List<CarPark> findNearestCarParksWithAvailabilityAsList(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Update car park total lots and available lots.
     * This aggregates data from car_park_availability table.
     */
    @Modifying
    @Transactional
    @Query("UPDATE CarPark cp SET cp.totalLots = :totalLots, cp.totalAvailableLots = :totalAvailableLots WHERE cp.id = :carParkId")
    void updateTotalLotsAndAvailableLots(@Param("carParkId") Long carParkId,
                                          @Param("totalLots") Integer totalLots,
                                          @Param("totalAvailableLots") Integer totalAvailableLots);
}
