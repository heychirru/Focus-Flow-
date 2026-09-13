package com.focusflowai.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiClient {

    private static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");
    private static final ApiClient INSTANCE = new ApiClient();

    private final HttpClient http;
    private final Gson gson;

    private ApiClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> LocalDateTime.parse(json.getAsString()))
                .create();
    }

    public static ApiClient getInstance() {
        return INSTANCE;
    }

    public <T> CompletableFuture<T> getAsync(String path, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = send(request(path).GET().timeout(Duration.ofSeconds(30)).build());
                if (response.statusCode() == 404) {
                    return null;
                }
                if (type == String.class) {
                    return type.cast(response.body());
                }
                return gson.fromJson(response.body(), type);
            } catch (Exception e) {
                throw new RuntimeException("GET " + path + " failed: " + e.getMessage(), e);
            }
        });
    }

    public <T> CompletableFuture<List<T>> getListAsync(String path,
                                                       com.google.gson.reflect.TypeToken<List<T>> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = send(request(path).GET().timeout(Duration.ofSeconds(30)).build());
                return gson.fromJson(response.body(), type.getType());
            } catch (Exception e) {
                throw new RuntimeException("GET " + path + " failed: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<String> getTextAsync(String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(request(path).GET().timeout(Duration.ofSeconds(30)).build()).body();
            } catch (Exception e) {
                throw new RuntimeException("GET " + path + " failed: " + e.getMessage(), e);
            }
        });
    }

    public <T> CompletableFuture<T> postAsync(String path, Object body, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> sendWithBody("POST", path, body, type, Duration.ofSeconds(60)));
    }

    public <T> CompletableFuture<T> putAsync(String path, Object body, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> sendWithBody("PUT", path, body, type, Duration.ofSeconds(30)));
    }

    public <T> CompletableFuture<T> patchAsync(String path, Object body, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> sendWithBody("PATCH", path, body, type, Duration.ofSeconds(30)));
    }

    public CompletableFuture<Void> deleteAsync(String path) {
        return CompletableFuture.runAsync(() -> {
            try {
                send(request(path).DELETE().timeout(Duration.ofSeconds(30)).build());
            } catch (Exception e) {
                throw new RuntimeException("DELETE " + path + " failed: " + e.getMessage(), e);
            }
        });
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder().uri(URI.create(BASE_URL + path));
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
    }

    private <T> T sendWithBody(String method, String path, Object body, Class<T> type, Duration timeout) {
        try {
            String json = body != null ? gson.toJson(body) : "";
            HttpRequest request = request(path)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json))
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = send(request);
            if (type == String.class) {
                return type.cast(response.body());
            }
            return gson.fromJson(response.body(), type);
        } catch (Exception e) {
            throw new RuntimeException(method + " " + path + " failed: " + e.getMessage(), e);
        }
    }
}
