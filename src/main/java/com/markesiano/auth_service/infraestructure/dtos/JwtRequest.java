package com.markesiano.auth_service.infraestructure.dtos;

import java.time.Duration;

public record JwtRequest(
    String clientId,
    String token,
    Duration ttl
) {

}
