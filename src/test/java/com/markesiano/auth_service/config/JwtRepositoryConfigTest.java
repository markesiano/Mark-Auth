package com.markesiano.auth_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.markesiano.auth_service.application.interfaces.RepositoryJwt;
import com.markesiano.auth_service.infraestructure.data.InMemoryJwtRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para verificar que la configuración de inyección de dependencias
 * funciona correctamente según las propiedades configuradas.
 */
public class JwtRepositoryConfigTest {

    @SpringBootTest
    @TestPropertySource(properties = {"jwt.repository.type=memory"})
    static class InMemoryRepositoryConfigTest {
        
        @Autowired
        private ApplicationContext context;
        
        @Autowired
        private RepositoryJwt<String> repositoryJwt;
        
        @Test
        void shouldUseInMemoryRepository() {
            // Verify that InMemoryJwtRepository is injected
            assertNotNull(repositoryJwt);
            assertTrue(repositoryJwt instanceof InMemoryJwtRepository);
            
            // Verify only one RepositoryJwt bean exists
            String[] beanNames = context.getBeanNamesForType(RepositoryJwt.class);
            assertEquals(1, beanNames.length);
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {})  // No jwt.repository.type property
    static class DefaultRepositoryConfigTest {
        
        @Autowired
        private ApplicationContext context;
        
        @Autowired
        private RepositoryJwt<String> repositoryJwt;
        
        @Test
        void shouldUseInMemoryRepositoryByDefault() {
            // When no jwt.repository.type is specified, should default to memory
            assertNotNull(repositoryJwt);
            assertTrue(repositoryJwt instanceof InMemoryJwtRepository);
            
            // Verify only one RepositoryJwt bean exists
            String[] beanNames = context.getBeanNamesForType(RepositoryJwt.class);
            assertEquals(1, beanNames.length);
        }
    }
}
