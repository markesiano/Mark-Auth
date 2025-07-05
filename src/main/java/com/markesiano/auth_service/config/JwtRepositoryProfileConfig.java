package com.markesiano.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;

import com.markesiano.auth_service.application.interfaces.RepositoryJwt;
import com.markesiano.auth_service.infraestructure.data.InMemoryJwtRepository;
import com.markesiano.auth_service.infraestructure.data.RedisJwtRepository;

/**
 * Alternative configuration using Spring Profiles for JWT Repository selection.
 * 
 * Usage:
 * - For production with Redis: --spring.profiles.active=redis
 * - For development/testing: --spring.profiles.active=dev (or no profile)
 * - For explicit in-memory: --spring.profiles.active=memory
 */
@Configuration
public class JwtRepositoryProfileConfig {
    
    @Bean
    @Primary
    @Profile("redis")
    @Qualifier("redisJwtRepo")
    public RepositoryJwt<String> redisJwtRepository(WebClient webClient) {
        return new RedisJwtRepository(webClient);
    }
    
    @Bean
    @Primary
    @Profile({"dev", "test", "memory", "default"})
    @Qualifier("inMemoryJwtRepo")
    public RepositoryJwt<String> inMemoryJwtRepository() {
        return new InMemoryJwtRepository();
    }
}
