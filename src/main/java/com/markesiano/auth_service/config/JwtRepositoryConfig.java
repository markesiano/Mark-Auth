package com.markesiano.auth_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import com.markesiano.auth_service.application.interfaces.RepositoryJwt;
import com.markesiano.auth_service.infraestructure.data.InMemoryJwtRepository;
import com.markesiano.auth_service.infraestructure.data.RedisJwtRepository;

/**
 * Configuration class for JWT Repository implementations.
 * 
 * This configuration automatically selects the appropriate JWT repository implementation
 * based on application properties:
 * 
 * - If 'jwt.repository.type=redis' is configured, RedisJwtRepository will be used
 * - If 'jwt.repository.type=memory' or no configuration is present, InMemoryJwtRepository will be used
 * 
 * You can also use Redis-specific properties:
 * - If 'cachedredis.uri' is configured, RedisJwtRepository will be used
 * - Otherwise, InMemoryJwtRepository will be used as fallback
 */
@Configuration
public class JwtRepositoryConfig {
    
    /**
     * Redis JWT Repository bean that will be selected when Redis is configured.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = "jwt.repository.type", 
        havingValue = "redis",
        matchIfMissing = false
    )
    public RepositoryJwt<String> redisJwtRepository(WebClient webClient) {
        return new RedisJwtRepository(webClient);
    }
    
    /**
     * Fallback JWT Repository (In-Memory) used when Redis is not configured.
     * This is the default implementation that will be used if no specific
     * repository type is configured.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = "jwt.repository.type", 
        havingValue = "memory",
        matchIfMissing = true
    )
    public RepositoryJwt<String> inMemoryJwtRepository() {
        return new InMemoryJwtRepository();
    }
    
    /**
     * Alternative configuration based on Redis URI presence.
     * This provides backward compatibility with existing configurations.
     */
    @Bean
    @ConditionalOnProperty(name = "cachedredis.uri")
    public RepositoryJwt<String> redisJwtRepositoryByUri(WebClient webClient) {
        return new RedisJwtRepository(webClient);
    }
}
