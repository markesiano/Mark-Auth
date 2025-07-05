package com.markesiano.auth_service.application.interfaces;

import java.util.concurrent.CompletableFuture;

public interface RepositoryJwt<TToken> {
    CompletableFuture<TToken> getJwtCached(String clientId);
    CompletableFuture<Void> saveJwt(String clientId, TToken token);
    
}
