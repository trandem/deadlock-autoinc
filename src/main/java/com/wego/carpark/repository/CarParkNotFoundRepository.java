package com.wego.carpark.repository;

import com.wego.carpark.model.CarParkNotFound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CarParkNotFound entity.
 * Tracks car parks from API that are not found in our database.
 */
@Repository
public interface CarParkNotFoundRepository extends JpaRepository<CarParkNotFound, Long>, CarParkNotFoundRepositoryCustom {

    /**
     * Find a not found record by car park number.
     */
    Optional<CarParkNotFound> findByCarParkNo(String carParkNo);

    /**
     * Find not found records by multiple car park numbers (batch query).
     */
    List<CarParkNotFound> findByCarParkNoIn(List<String> carParkNos);
}