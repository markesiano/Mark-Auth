package com.markesiano.auth_service.infraestructure.data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.markesiano.auth_service.application.interfaces.RepositoryJwt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class InMemoryJwtRepository implements RepositoryJwt<String> {

    private final Map<String, TokenEntry> jwtCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static class TokenEntry {
        private final String token;
        private final LocalDateTime expirationTime;
        
        public TokenEntry(String token) {
            this.token = token;
            this.expirationTime = LocalDateTime.now().plusHours(1);
        }
        
        public String getToken() {
            return token;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expirationTime);
        }
    }

    @PostConstruct
    public void init() {
        // Schedule cleanup task every 30 minutes
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 30, 30, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }

    @Override
    public CompletableFuture<String> getJwtCached(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
        return CompletableFuture.supplyAsync(() -> {
            TokenEntry entry = jwtCache.get(clientId);
            if (entry != null && !entry.isExpired()) {
                return entry.getToken();
            } else if (entry != null && entry.isExpired()) {
                // Remove expired token
                jwtCache.remove(clientId);
                return null;
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> saveJwt(String clientId, String token) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        return CompletableFuture.runAsync(() -> {
            jwtCache.put(clientId, new TokenEntry(token));
        });
    }

    public void clear() {
        jwtCache.clear();
    }

    private void cleanupExpiredTokens() {
        jwtCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    // Method for testing purposes
    public int getCacheSize() {
        return jwtCache.size();
    }
}
