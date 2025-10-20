package com.wego.carpark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Car Park Availability API.
 * This Spring Boot application provides REST endpoints to find the nearest car parks
 * with available parking lots based on user's geographical location.
 */
@SpringBootApplication
public class CarparkApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarparkApiApplication.class, args);
    }
}
