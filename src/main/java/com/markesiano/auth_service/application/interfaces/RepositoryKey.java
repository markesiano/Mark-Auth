package com.markesiano.auth_service.application.interfaces;

import java.util.concurrent.CompletableFuture;

public interface RepositoryKey {
    CompletableFuture<Boolean> isValidClient(String clientId, String productKey);
}
