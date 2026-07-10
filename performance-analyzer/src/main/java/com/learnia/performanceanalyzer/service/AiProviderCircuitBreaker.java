package com.learnia.performanceanalyzer.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class AiProviderCircuitBreaker {

    private static final Duration OPEN_DURATION = Duration.ofMinutes(1);

    private final ConcurrentMap<String, Instant> openUntilByProvider = new ConcurrentHashMap<>();

    public boolean allowsRequest(String provider) {
        Instant openUntil = openUntilByProvider.get(provider);
        if (openUntil == null) {
            return true;
        }
        if (Instant.now().isBefore(openUntil)) {
            return false;
        }
        openUntilByProvider.remove(provider, openUntil);
        return true;
    }

    public void recordUnavailable(String provider) {
        openUntilByProvider.put(provider, Instant.now().plus(OPEN_DURATION));
    }

    public void recordSuccess(String provider) {
        openUntilByProvider.remove(provider);
    }
}
