package com.auth.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.markesiano.auth_service.infraestructure.data.InMemoryJwtRepository;

public class InMemoryJwtRepositoryTest {
    private InMemoryJwtRepository repository;

    @BeforeEach
    public void setUp() {
        this.repository = new InMemoryJwtRepository();
    }

    @Test
    public void testSaveAndGetJwtSuccess() {
        // Arrange
        String clientId = "testClient";
        String token = "jwt.token.here";

        // Act
        CompletableFuture<Void> saveFuture = repository.saveJwt(clientId, token);
        saveFuture.join(); // Wait for save to complete

        CompletableFuture<String> getFuture = repository.getJwtCached(clientId);
        String retrievedToken = getFuture.join();

        // Assert
        assertNotNull(retrievedToken);
        assertEquals(token, retrievedToken);
    }

    @Test
    public void testGetJwtCachedNotFound() {
        // Arrange
        String nonExistentClientId = "nonExistentClient";

        // Act
        CompletableFuture<String> future = repository.getJwtCached(nonExistentClientId);
        String retrievedToken = future.join();

        // Assert
        assertNull(retrievedToken);
    }

    @Test
    public void testSaveJwtOverwrite() {
        // Arrange
        String clientId = "testClient";
        String originalToken = "original.jwt.token";
        String newToken = "new.jwt.token";

        // Act
        repository.saveJwt(clientId, originalToken).join();
        repository.saveJwt(clientId, newToken).join();
        
        String retrievedToken = repository.getJwtCached(clientId).join();

        // Assert
        assertEquals(newToken, retrievedToken);
        assertNotEquals(originalToken, retrievedToken);
    }

    @Test
    public void testSaveJwtWithNullClientId() {
        // Arrange
        String token = "jwt.token.here";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> repository.saveJwt(null, token));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveJwtWithEmptyClientId() {
        // Arrange
        String emptyClientId = "";
        String token = "jwt.token.here";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> repository.saveJwt(emptyClientId, token));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveJwtWithNullToken() {
        // Arrange
        String clientId = "testClient";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> repository.saveJwt(clientId, null));
        
        assertEquals("Token cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveJwtWithEmptyToken() {
        // Arrange
        String clientId = "testClient";
        String emptyToken = "";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> repository.saveJwt(clientId, emptyToken));
        
        assertEquals("Token cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetJwtCachedWithNullClientId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> repository.getJwtCached(null));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetJwtCachedWithEmptyClientId() {
        // Arrange
        String emptyClientId = "";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> repository.getJwtCached(emptyClientId));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testMultipleClientsIndependentStorage() {
        // Arrange
        String clientId1 = "client1";
        String clientId2 = "client2";
        String token1 = "token.for.client1";
        String token2 = "token.for.client2";

        // Act
        repository.saveJwt(clientId1, token1).join();
        repository.saveJwt(clientId2, token2).join();

        String retrievedToken1 = repository.getJwtCached(clientId1).join();
        String retrievedToken2 = repository.getJwtCached(clientId2).join();

        // Assert
        assertEquals(token1, retrievedToken1);
        assertEquals(token2, retrievedToken2);
        assertNotEquals(retrievedToken1, retrievedToken2);
    }

    @Test
    public void testAsynchronousOperations() throws ExecutionException, InterruptedException {
        // Arrange
        String clientId = "asyncClient";
        String token = "async.token";

        // Act - Don't join immediately to test async behavior
        CompletableFuture<Void> saveFuture = repository.saveJwt(clientId, token);
        CompletableFuture<String> getFuture = saveFuture.thenCompose(v -> 
            repository.getJwtCached(clientId));

        // Assert
        String result = getFuture.get(); // Use get() instead of join() for exception handling
        assertEquals(token, result);
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        // This test verifies that ConcurrentHashMap handles concurrent access properly
        String baseClientId = "client";
        String baseToken = "token";
        int threadCount = 10;

        Thread[] saveThreads = new Thread[threadCount];
        Thread[] getThreads = new Thread[threadCount];
        String[] retrievedTokens = new String[threadCount];

        // Create save threads
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            saveThreads[i] = new Thread(() -> {
                String clientId = baseClientId + index;
                String token = baseToken + index;
                repository.saveJwt(clientId, token).join();
            });
        }

        // Create get threads
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            getThreads[i] = new Thread(() -> {
                String clientId = baseClientId + index;
                retrievedTokens[index] = repository.getJwtCached(clientId).join();
            });
        }

        // Start all save threads
        for (Thread thread : saveThreads) {
            thread.start();
        }

        // Wait for all save threads to complete
        for (Thread thread : saveThreads) {
            thread.join();
        }

        // Start all get threads
        for (Thread thread : getThreads) {
            thread.start();
        }

        // Wait for all get threads to complete
        for (Thread thread : getThreads) {
            thread.join();
        }

        // Assert all tokens were stored and retrieved correctly
        for (int i = 0; i < threadCount; i++) {
            String expectedToken = baseToken + i;
            assertEquals(expectedToken, retrievedTokens[i]);
        }
    }

    @Test
    public void testCacheSizeMethod() {
        // Test the getCacheSize method for testing purposes
        assertEquals(0, repository.getCacheSize());
        
        repository.saveJwt("client1", "token1").join();
        assertEquals(1, repository.getCacheSize());
        
        repository.saveJwt("client2", "token2").join();
        assertEquals(2, repository.getCacheSize());
        
        repository.clear();
        assertEquals(0, repository.getCacheSize());
    }

    @Test
    public void testScheduledCleanupInitialization() {
        // Test that the repository initializes properly without errors
        InMemoryJwtRepository newRepo = new InMemoryJwtRepository();
        newRepo.init(); // Should not throw any exception
        
        // Save a token and verify it works
        String clientId = "testClient";
        String token = "testToken";
        
        newRepo.saveJwt(clientId, token).join();
        String retrievedToken = newRepo.getJwtCached(clientId).join();
        
        assertEquals(token, retrievedToken);
        
        // Clean up
        newRepo.destroy();
    }

    @Test
    public void testRepositoryDestroyMethod() {
        // Test that destroy method can be called without issues
        assertDoesNotThrow(() -> {
            repository.destroy();
        });
    }

    @Test
    public void testTokenExpirationLogic() {
        // This test verifies the expiration logic exists but doesn't wait for actual expiration
        // since that would take 1 hour
        String clientId = "expirationTest";
        String token = "expiring-token";
        
        // Save token
        repository.saveJwt(clientId, token).join();
        
        // Immediately retrieve - should be available
        String retrievedToken = repository.getJwtCached(clientId).join();
        assertEquals(token, retrievedToken);
        
        // Verify cache size
        assertTrue(repository.getCacheSize() > 0);
    }

}
