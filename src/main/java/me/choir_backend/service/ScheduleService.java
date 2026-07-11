package me.choir_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Service
public class ScheduleService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final URI icsUri;

    private volatile String cachedIcs;
    private volatile Instant cachedAt = Instant.EPOCH;

    public ScheduleService(@Value("${app.schedule-ics-url}") String icsUrl) {
        this.icsUri = URI.create(icsUrl);
    }

    public synchronized String fetchIcs() {
        if (cachedIcs != null && Instant.now().isBefore(cachedAt.plus(CACHE_TTL))) {
            return cachedIcs;
        }
        cachedIcs = fetchFromSource();
        cachedAt = Instant.now();
        return cachedIcs;
    }

    private String fetchFromSource() {
        HttpRequest request = HttpRequest.newBuilder(icsUri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Failed to fetch calendar feed: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch calendar feed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch calendar feed", e);
        }
    }
}
