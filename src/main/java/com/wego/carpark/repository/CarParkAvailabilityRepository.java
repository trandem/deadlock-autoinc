package com.wego.carpark.repository;

import com.wego.carpark.model.CarParkAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CarParkAvailability entity.
 * Provides methods to manage car park availability data.
 */
@Repository
public interface CarParkAvailabilityRepository extends JpaRepository<CarParkAvailability, Long>, CarParkAvailabilityRepositoryCustom {

    /**
     * Find availability by car park ID and lot type.
     */
    Optional<CarParkAvailability> findByCarParkIdAndLotType(Long carParkId, String lotType);

    /**
     * Find all availabilities for a specific car park.
     */
    List<CarParkAvailability> findByCarParkId(Long carParkId);

    /**
     * Find all availabilities for multiple car park IDs.
     */
    List<CarParkAvailability> findByCarParkIdIn(List<Long> carParkIds);

    /**
     * Find availability by car park number and lot type.
     */
    Optional<CarParkAvailability> findByCarParkNoAndLotType(String carParkNo, String lotType);

    /**
     * Delete all availability records for a specific car park number.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CarParkAvailability cpa WHERE cpa.carParkNo = :carParkNo")
    void deleteByCarParkNo(@Param("carParkNo") String carParkNo);

    /**
     * Get aggregated availability for a car park (summing all lot types).
     */
    @Query("""
        SELECT SUM(cpa.totalLots), SUM(cpa.availableLots)
        FROM CarParkAvailability cpa
        WHERE cpa.carParkId = :carParkId
        """)
    Object[] getAggregatedAvailability(@Param("carParkId") Long carParkId);
}
