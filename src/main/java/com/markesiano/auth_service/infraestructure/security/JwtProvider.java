package com.markesiano.auth_service.infraestructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.markesiano.auth_service.application.interfaces.JwtKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
@Component
public class JwtProvider implements JwtKey{
    @Value("${spring.security.jwt.secret}")
    private String secret;
    private final long expirationTime = 3600000; // 1 hour

    @Override
    public CompletableFuture<String> generateToken(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
        return CompletableFuture.supplyAsync(() -> {
            return Jwts.builder()
                .setSubject(clientId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)),SignatureAlgorithm.HS256)
                .compact();
        });

    }

}
