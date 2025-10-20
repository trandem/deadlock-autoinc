package com.wego.carpark.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to track car parks that are returned from the API but not found in our database.
 * This helps identify missing car park data that needs to be imported.
 */
@Entity
@Table(name = "car_park_not_found")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarParkNotFound {

    @Id
    private Long id;

    @Column(name = "car_park_no", nullable = false, length = 50)
    private String carParkNo;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;

    @PrePersist
    protected void onCreate() {
        if (firstSeenAt == null) {
            firstSeenAt = LocalDateTime.now();
        }
        if (lastSeenAt == null) {
            lastSeenAt = LocalDateTime.now();
        }
        if (occurrenceCount == null) {
            occurrenceCount = 1;
        }
    }
}