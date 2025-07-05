package com.markesiano.auth_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markesiano.auth_service.application.usecases.interfaces.AuthenticateClientUseCase;
import com.markesiano.auth_service.infraestructure.dtos.AuthRequest;
import com.markesiano.auth_service.infraestructure.dtos.AuthResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/auth")
public class AuthController {
    

    private final AuthenticateClientUseCase<AuthResponse, AuthRequest> authenticationClientUseCase;

    public AuthController(AuthenticateClientUseCase<AuthResponse, AuthRequest> authenticationClientUseCase) {
        this.authenticationClientUseCase = authenticationClientUseCase;
    }

    @PostMapping("/token")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(
                AuthResponse.error("Bad request", "Request body cannot be null"));
        }
        if (request.clientId() == null || request.clientId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                AuthResponse.error("Bad request", "Client ID cannot be null or empty"));
        }
        if (request.productKey() == null || request.productKey().isEmpty()) {
            return ResponseEntity.badRequest().body(
                AuthResponse.error("Bad request", "Product Key cannot be null or empty"));
        }
        
        return authenticationClientUseCase.authenticate(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> ResponseEntity.status(401).body(
                AuthResponse.error("Authentication failed", ex.getCause().getMessage())))
            .join();
    }
}
