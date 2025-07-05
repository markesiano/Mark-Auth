package com.markesiano.auth_service.infraestructure.data;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import com.markesiano.auth_service.application.interfaces.RepositoryJwt;
import com.markesiano.auth_service.infraestructure.dtos.JwtRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RedisJwtRepository implements RepositoryJwt<String> {
    private final WebClient webClient;

    @Override
    public CompletableFuture<String> getJwtCached(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
        return CompletableFuture.supplyAsync(() -> {
            return webClient.get()
                .uri("/api/v1/cached-redis/token/{clientId}", clientId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        clientResponse -> Mono.error(new TokenNotFoundException()))
                .bodyToMono(String.class)
                .onErrorResume(TokenNotFoundException.class, ex -> Mono.empty())
                .block();
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
        JwtRequest jwtRequest = new JwtRequest(clientId, token, Duration.ofHours(1)); // Match JWT expiration time
        return CompletableFuture.runAsync(() -> {
            webClient.post()
                    .uri("/api/v1/cached-redis/token")
                    .bodyValue(jwtRequest)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.createException())
                    .bodyToMono(Void.class)
                    .block();
        });
    }
    public class TokenNotFoundException extends RuntimeException {
        public TokenNotFoundException() {
            super("Token not found");
    }
}
}
