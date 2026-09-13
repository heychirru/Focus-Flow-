package com.focusflow.coach.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client for calling Session Service's /api/sessions/summary endpoint.
 */
@Component
public class SessionServiceClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${coach.session-service-url}")
    private String sessionServiceUrl;

    /**
     * Fetches the plain-text summary of session stats from the Session Service.
     */
    public String getSummary() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sessionServiceUrl + "/api/sessions/summary"))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Session Service returned " + response.statusCode());
            }
            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Session Service call interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reach Session Service: " + e.getMessage(), e);
        }
    }
}
