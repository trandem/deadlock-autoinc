package com.wego.carpark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.carpark.dto.CarParkAvailabilityApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service for fetching car park availability data from external API.
 */
@Service
@Slf4j
public class CarParkDataFetcher {

    private static final String API_URL = "https://api.data.gov.sg/v1/transport/carpark-availability";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CarParkDataFetcher() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public CarParkAvailabilityApiResponse fetchAvailabilityData() throws IOException, InterruptedException {
        var jsonResponse = fetchFromApi();
        return parseResponse(jsonResponse);
    }

    private String fetchFromApi() throws IOException, InterruptedException {
        log.info("Fetching data from: {}", API_URL);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/json")
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status code: " + response.statusCode());
        }

        return response.body();
    }

    private CarParkAvailabilityApiResponse parseResponse(String jsonResponse) throws IOException {
        return objectMapper.readValue(jsonResponse, CarParkAvailabilityApiResponse.class);
    }
}
