package com.markesiano.auth_service.infraestructure.dtos;

public record AuthRequest(
    String clientId,
    String productKey
) {

}
