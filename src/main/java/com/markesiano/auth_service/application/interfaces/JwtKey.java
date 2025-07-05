package com.markesiano.auth_service.application.interfaces;

import java.util.concurrent.CompletableFuture;

public interface JwtKey {
    CompletableFuture<String> generateToken(String clientId);
}
