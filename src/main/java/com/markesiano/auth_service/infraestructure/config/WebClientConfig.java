package com.markesiano.auth_service.infraestructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {
    @Value("${cachedredis.uri:}")
    private String redisUri;
    @Value("${cachedredis.port:6379}")
    private int redisPort;

    @Bean
    @ConditionalOnProperty(name = "cachedredis.uri")
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(redisUri + ":" + redisPort)
                .build();
    }

}
