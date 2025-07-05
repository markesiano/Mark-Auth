package com.markesiano.auth_service.infraestructure.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    String token,
    String error,
    String message
) {
    // Constructor for successful authentication (token only)
    public AuthResponse(String token) {
        this(token, null, null);
    }
    
    // Constructor for error responses (error and message)
    public static AuthResponse error(String error, String message) {
        return new AuthResponse(null, error, message);
    }
    
    // Constructor for simple error (backward compatibility)
    public static AuthResponse simpleError(String message) {
        return new AuthResponse(null, "Authentication failed", message);
    }
}
