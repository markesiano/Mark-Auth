package com.markesiano.auth_service.infraestructure.data;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.markesiano.auth_service.application.interfaces.RepositoryKey;

@Repository
public class InMemoryClientKeyRepository implements RepositoryKey{

    private final Map<String, String> keys = new ConcurrentHashMap<>();

    // Pre-populated keys for demonstration purposes
    // In a real application, these would be stored in a database or secure storage
    public InMemoryClientKeyRepository() {
        keys.put("markepos01", "productKey1");
        keys.put("markepos02", "productKey2");
    }

    @Override
    public CompletableFuture<Boolean> isValidClient(String clientId, String productKey) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        if (productKey == null) {
            throw new IllegalArgumentException("Product key cannot be null");
        }
        return CompletableFuture.supplyAsync(() -> {
            String storedKey = keys.get(clientId);
            return storedKey != null && storedKey.equals(productKey);
        });
    }
    public void addClientKey(String clientId, String productKey) {
        keys.put(clientId, productKey);
    }
    public void removeClientKey(String clientId) {
        keys.remove(clientId);
    }
    public void clear() {
        keys.clear();
    }
    
}
