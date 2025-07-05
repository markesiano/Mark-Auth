package com.auth.data;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.markesiano.auth_service.infraestructure.data.RedisJwtRepository;

@ExtendWith(MockitoExtension.class)
public class RedisJwtRepositoryTest {

    @Mock
    private WebClient webClient;

    private RedisJwtRepository repository;

    @BeforeEach
    public void setUp() {
        repository = new RedisJwtRepository(webClient);
    }

    // Input Validation Tests
    @Test
    public void testGetJwtCachedWithNullClientId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repository.getJwtCached(null));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
        verifyNoInteractions(webClient);
    }

    @Test
    public void testGetJwtCachedWithEmptyClientId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repository.getJwtCached(""));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
        verifyNoInteractions(webClient);
    }

    @Test
    public void testSaveJwtWithNullClientId() {
        // Arrange
        String token = "jwt.token.here";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repository.saveJwt(null, token));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
        verifyNoInteractions(webClient);
    }

    @Test
    public void testSaveJwtWithEmptyClientId() {
        // Arrange
        String token = "jwt.token.here";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repository.saveJwt("", token));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
        verifyNoInteractions(webClient);
    }

    @Test
    public void testSaveJwtWithNullToken() {
        // Arrange
        String clientId = "testClient";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repository.saveJwt(clientId, null));
        
        assertEquals("Token cannot be null or empty", exception.getMessage());
        verifyNoInteractions(webClient);
    }

    @Test
    public void testSaveJwtWithEmptyToken() {
        // Arrange
        String clientId = "testClient";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repository.saveJwt(clientId, ""));
        
        assertEquals("Token cannot be null or empty", exception.getMessage());
        verifyNoInteractions(webClient);
    }

    // Async Behavior Tests
    @Test
    public void testGetJwtCachedReturnsCompletableFuture() {
        // Arrange
        String clientId = "testClient";

        // Act
        CompletableFuture<String> result = repository.getJwtCached(clientId);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof CompletableFuture);
    }

    @Test
    public void testSaveJwtReturnsCompletableFuture() {
        // Arrange
        String clientId = "testClient";
        String token = "testToken";

        // Act
        CompletableFuture<Void> result = repository.saveJwt(clientId, token);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof CompletableFuture);
    }

    // Exception Handling Tests
    @Test
    public void testGetJwtCachedHandlesWebClientException() {
        // This test verifies that WebClient exceptions are properly wrapped
        // We cannot easily mock the entire WebClient chain, but we can test
        // that the method completes exceptionally when WebClient fails
        
        String clientId = "testClient";
        
        // Create a real WebClient that will fail
        WebClient failingWebClient = WebClient.builder()
            .baseUrl("http://non-existent-host-12345.com")
            .build();
        
        RedisJwtRepository failingRepository = new RedisJwtRepository(failingWebClient);
        
        // Act
        CompletableFuture<String> future = failingRepository.getJwtCached(clientId);
        
        // Assert
        assertThrows(CompletionException.class, () -> future.join());
    }

    @Test
    public void testSaveJwtHandlesWebClientException() {
        // This test verifies that WebClient exceptions are properly wrapped
        String clientId = "testClient";
        String token = "testToken";
        
        // Create a real WebClient that will fail
        WebClient failingWebClient = WebClient.builder()
            .baseUrl("http://non-existent-host-12345.com")
            .build();
        
        RedisJwtRepository failingRepository = new RedisJwtRepository(failingWebClient);
        
        // Act
        CompletableFuture<Void> future = failingRepository.saveJwt(clientId, token);
        
        // Assert
        assertThrows(CompletionException.class, () -> future.join());
    }

    // TokenNotFoundException Tests
    @Test
    public void testTokenNotFoundExceptionCreation() {
        // Create a repository instance to access the inner class
        RedisJwtRepository testRepository = new RedisJwtRepository(webClient);
        
        // Act
        RedisJwtRepository.TokenNotFoundException exception = 
            testRepository.new TokenNotFoundException();

        // Assert
        assertEquals("Token not found", exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    // Concurrency Tests
    @Test
    public void testConcurrentGetJwtCachedCalls() throws InterruptedException {
        // Test that multiple concurrent calls can be made without issues
        String clientId = "concurrentClient";
        int threadCount = 5;
        
        Thread[] threads = new Thread[threadCount];
        @SuppressWarnings("unchecked")
        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];
        
        // Create threads that make concurrent calls
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                futures[index] = repository.getJwtCached(clientId + index);
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Assert all futures were created
        for (CompletableFuture<String> future : futures) {
            assertNotNull(future);
        }
    }

    @Test
    public void testConcurrentSaveJwtCalls() throws InterruptedException {
        // Test that multiple concurrent calls can be made without issues
        String clientId = "concurrentClient";
        String token = "concurrentToken";
        int threadCount = 5;
        
        Thread[] threads = new Thread[threadCount];
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        
        // Create threads that make concurrent calls
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                futures[index] = repository.saveJwt(clientId + index, token + index);
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Assert all futures were created
        for (CompletableFuture<Void> future : futures) {
            assertNotNull(future);
        }
    }

}
