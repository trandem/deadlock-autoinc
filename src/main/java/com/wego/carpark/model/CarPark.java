package com.wego.carpark.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing a car park with its static information.
 * Contains geographical coordinates and various attributes of the parking facility.
 */
@Entity
@Table(name = "car_parks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarPark {

    @Id
    private Long id;

    @Column(name = "car_park_no", unique = true, nullable = false, length = 50)
    private String carParkNo;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "car_park_type", length = 50)
    private String carParkType;

    @Column(name = "type_of_parking_system", length = 100)
    private String typeOfParkingSystem;

    @Column(name = "short_term_parking", length = 50)
    private String shortTermParking;

    @Column(name = "parking_hours_from")
    private LocalTime parkingHoursFrom;

    @Column(name = "parking_hours_to")
    private LocalTime parkingHoursTo;

    @Column(name = "free_parking", length = 50)
    private String freeParking;

    @Column(name = "night_parking", length = 50)
    private String nightParking;

    @Column(name = "car_park_decks")
    private Integer carParkDecks;

    @Column(name = "gantry_height", precision = 5, scale = 2)
    private BigDecimal gantryHeight;

    @Column(name = "car_park_basement", length = 10)
    private String carParkBasement;

    @Column(name = "total_lots")
    private Integer totalLots;

    @Column(name = "total_available_lots")
    private Integer totalAvailableLots;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
