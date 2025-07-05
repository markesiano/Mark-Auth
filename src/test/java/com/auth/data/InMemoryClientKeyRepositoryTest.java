package com.auth.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;

import com.markesiano.auth_service.infraestructure.data.InMemoryClientKeyRepository;

public class InMemoryClientKeyRepositoryTest {

    private InMemoryClientKeyRepository repository;

    @BeforeEach
    public void setUp() {
        this.repository = new InMemoryClientKeyRepository();
    }

    @Test
    public void testIsValidClientSuccess() {
        // Arrange
        String clientId = "markepos01";
        String productKey = "productKey1";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(clientId, productKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertTrue(isValid, "Expected client to be valid with correct product key");
    }

    @Test
    public void testIsValidClientSuccessSecondClient() {
        // Arrange
        String clientId = "markepos02";
        String productKey = "productKey2";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(clientId, productKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertTrue(isValid, "Expected second client to be valid with correct product key");
    }

    @Test
    public void testIsValidClientFailureInvalidClientId() {
        // Arrange
        String invalidClientId = "invalidClient";
        String productKey = "productKey1";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(invalidClientId, productKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertFalse(isValid, "Expected client to be invalid with non-existent client ID");
    }

    @Test
    public void testIsValidClientFailureInvalidProductKey() {
        // Arrange
        String clientId = "markepos01";
        String invalidProductKey = "wrongKey";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(clientId, invalidProductKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertFalse(isValid, "Expected client to be invalid with wrong product key");
    }

    @Test
    public void testIsValidClientFailureBothInvalid() {
        // Arrange
        String invalidClientId = "invalidClient";
        String invalidProductKey = "wrongKey";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(invalidClientId, invalidProductKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertFalse(isValid, "Expected client to be invalid with both invalid client ID and product key");
    }

    @Test
    public void testIsValidClientWithNullClientId() {
        // Arrange
        String productKey = "productKey1";

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repository.isValidClient(null, productKey);
        });

        // Assert
        assertTrue(exception instanceof IllegalArgumentException, "Expected IllegalArgumentException for null client ID");
    }

    @Test
    public void testIsValidClientWithNullProductKey() {
        // Arrange
        String clientId = "markepos01";

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repository.isValidClient(clientId, null);
        });

        // Assert
        assertTrue(exception instanceof IllegalArgumentException, "Expected IllegalArgumentException for null product key");    
    }

    @Test
    public void testIsValidClientWithBothNull() {
        // Act
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repository.isValidClient(null, null);
        });

        
        // Assert
        assertTrue(exception instanceof IllegalArgumentException, "Expected IllegalArgumentException for null client ID and product key");
    }

    @Test
    public void testIsValidClientWithEmptyClientId() {
        // Arrange
        String emptyClientId = "";
        String productKey = "productKey1";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(emptyClientId, productKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertFalse(isValid, "Expected client to be invalid with empty client ID");
    }

    @Test
    public void testIsValidClientWithEmptyProductKey() {
        // Arrange
        String clientId = "markepos01";
        String emptyProductKey = "";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(clientId, emptyProductKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertFalse(isValid, "Expected client to be invalid with empty product key");
    }

    @Test
    public void testIsValidClientCaseSensitive() {
        // Arrange
        String clientIdUpperCase = "MARKEPOS01";
        String productKey = "productKey1";

        // Act
        CompletableFuture<Boolean> future = repository.isValidClient(clientIdUpperCase, productKey);
        boolean isValid = future.join();

        // Assert
        assertNotNull(future);
        assertFalse(isValid, "Expected client validation to be case sensitive");
    }

    @Test
    public void testAddClientKey() {
        // Arrange
        String newClientId = "markepos03";
        String newProductKey = "productKey3";

        // Act
        repository.addClientKey(newClientId, newProductKey);
        CompletableFuture<Boolean> future = repository.isValidClient(newClientId, newProductKey);
        boolean isValid = future.join();

        // Assert
        assertTrue(isValid, "Expected newly added client to be valid");
    }

    @Test
    public void testAddClientKeyOverwrite() {
        // Arrange
        String existingClientId = "markepos01";
        String newProductKey = "newProductKey";

        // Act - Overwrite existing key
        repository.addClientKey(existingClientId, newProductKey);
        
        // Test with new key
        CompletableFuture<Boolean> futureNew = repository.isValidClient(existingClientId, newProductKey);
        boolean isValidNew = futureNew.join();
        
        // Test with old key
        CompletableFuture<Boolean> futureOld = repository.isValidClient(existingClientId, "productKey1");
        boolean isValidOld = futureOld.join();

        // Assert
        assertTrue(isValidNew, "Expected client to be valid with new product key");
        assertFalse(isValidOld, "Expected client to be invalid with old product key after overwrite");
    }

    @Test
    public void testRemoveClientKey() {
        // Arrange
        String clientId = "markepos01";
        String productKey = "productKey1";

        // Verify client exists first
        assertTrue(repository.isValidClient(clientId, productKey).join());

        // Act
        repository.removeClientKey(clientId);
        CompletableFuture<Boolean> future = repository.isValidClient(clientId, productKey);
        boolean isValid = future.join();

        // Assert
        assertFalse(isValid, "Expected client to be invalid after removal");
    }

    @Test
    public void testRemoveNonExistentClientKey() {
        // Arrange
        String nonExistentClientId = "nonExistent";

        // Act - Should not throw exception
        assertDoesNotThrow(() -> repository.removeClientKey(nonExistentClientId));

        // Verify other clients still exist
        assertTrue(repository.isValidClient("markepos01", "productKey1").join());
        assertTrue(repository.isValidClient("markepos02", "productKey2").join());
    }

    @Test
    public void testClear() {
        // Arrange - Verify clients exist first
        assertTrue(repository.isValidClient("markepos01", "productKey1").join());
        assertTrue(repository.isValidClient("markepos02", "productKey2").join());

        // Act
        repository.clear();

        // Assert - All clients should be invalid
        assertFalse(repository.isValidClient("markepos01", "productKey1").join());
        assertFalse(repository.isValidClient("markepos02", "productKey2").join());
    }

    @Test
    public void testClearAndAddNew() {
        // Arrange
        repository.clear();
        String newClientId = "newClient";
        String newProductKey = "newKey";

        // Act
        repository.addClientKey(newClientId, newProductKey);

        // Assert
        assertTrue(repository.isValidClient(newClientId, newProductKey).join());
        assertFalse(repository.isValidClient("markepos01", "productKey1").join());
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        // This test verifies that ConcurrentHashMap handles concurrent access properly
        String clientId = "concurrentTest";
        String productKey = "concurrentKey";

        // Add key
        repository.addClientKey(clientId, productKey);

        // Create multiple threads that access the repository simultaneously
        Thread[] threads = new Thread[10];
        boolean[] results = new boolean[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = repository.isValidClient(clientId, productKey).join();
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

        // Assert all results are true
        for (boolean result : results) {
            assertTrue(result, "Expected all concurrent access attempts to return true");
        }
    }


}
