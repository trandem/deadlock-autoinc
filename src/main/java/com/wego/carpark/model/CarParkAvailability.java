package com.wego.carpark.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing real-time car park availability information.
 * This data is updated periodically from the external API.
 */
@Entity
@Table(name = "car_park_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarParkAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_park_id", nullable = false)
    private Long carParkId;

    @Column(name = "car_park_no", nullable = false, length = 50)
    private String carParkNo;

    @Column(name = "total_lots", nullable = false)
    private Integer totalLots;

    @Column(name = "available_lots", nullable = false)
    private Integer availableLots;

    @Column(name = "lot_type", nullable = false, length = 10)
    private String lotType;

    @Column(name = "update_datetime", nullable = false)
    private LocalDateTime updateDatetime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
