package com.wego.carpark.controller;

import com.wego.carpark.task.CarParkAvailabilitySyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin controller for manual data operations.
 * Provides endpoints to trigger data sync tasks manually.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CarParkAvailabilitySyncTask availabilitySyncTask;

    /**
     * Manually trigger car park availability data sync.
     * POST /admin/sync-availability
     *
     * @return Response indicating sync completion
     */
    @PostMapping("/sync-availability")
    public ResponseEntity<Map<String, String>> syncAvailability() {
        log.info("Manual availability sync triggered");

        try {
            availabilitySyncTask.syncAvailabilityData();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Car park availability data synced successfully"
            ));
        } catch (Exception e) {
            log.error("Error during manual sync", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to sync availability data: " + e.getMessage()
            ));
        }
    }
}
