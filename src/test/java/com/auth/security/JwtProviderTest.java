package com.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.markesiano.auth_service.infraestructure.security.JwtProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtProviderTest {

    private JwtProvider jwtProvider;
    private String testSecret;

    @BeforeEach
    public void setUp() {
        jwtProvider = new JwtProvider();
        
        // Generate a secure random secret for testing (minimum 256 bits for HS256)
        testSecret = generateSecureTestSecret();
        
        // Inject the test secret using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtProvider, "secret", testSecret);
    }

    /**
     * Generates a cryptographically secure random secret for testing purposes.
     * This ensures each test run uses a different secret and no hardcoded values are exposed.
     */
    private String generateSecureTestSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] secretBytes = new byte[64]; // 512 bits - more than enough for HS256
        secureRandom.nextBytes(secretBytes);
        return Base64.getEncoder().encodeToString(secretBytes);
    }

    // Input Validation Tests
    @Test
    public void testGenerateTokenWithNullClientId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> jwtProvider.generateToken(null));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGenerateTokenWithEmptyClientId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> jwtProvider.generateToken(""));
        
        assertEquals("Client ID cannot be null or empty", exception.getMessage());
    }

    // Successful Token Generation Tests
    @Test
    public void testGenerateTokenWithValidClientId() {
        // Arrange
        String clientId = "testClient123";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."), "JWT token should contain dots as separators");
        
        // Verify token structure (should have 3 parts separated by dots)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT token should have 3 parts (header.payload.signature)");
    }

    @Test
    public void testGenerateTokenContainsCorrectClientId() {
        // Arrange
        String clientId = "testClient123";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert - Parse the token to verify client ID
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        assertEquals(clientId, claims.getSubject());
    }

    @Test
    public void testGenerateTokenContainsIssuedAtDate() {
        // Arrange
        String clientId = "testClient123";
        Date beforeGeneration = new Date();

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();
        Date afterGeneration = new Date();

        // Assert - Parse the token to verify issued at date
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        Date issuedAt = claims.getIssuedAt();
        assertNotNull(issuedAt);
        // Allow a tolerance window of 1 seconds before and after
        long toleranceMillis = 1000;
        assertTrue(
            issuedAt.getTime() >= beforeGeneration.getTime() - toleranceMillis &&
            issuedAt.getTime() <= afterGeneration.getTime() + toleranceMillis,
            "Issued at date should be within the tolerance window of before and after generation times"
        );

    }

    @Test
    public void testGenerateTokenContainsExpirationDate() {
        // Arrange
        String clientId = "testClient123";
        long expectedExpirationTime = 3600000; // 1 hour

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert - Parse the token to verify expiration date
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        
        assertNotNull(expiration);
        assertTrue(expiration.after(issuedAt));
        
        // Check that expiration is approximately 1 hour after issued at
        long actualExpirationTime = expiration.getTime() - issuedAt.getTime();
        // Allow for small time differences due to execution time
        assertTrue(Math.abs(actualExpirationTime - expectedExpirationTime) < 1000, 
            "Expiration time should be approximately 1 hour");
    }

    @Test
    public void testGenerateTokenWithDifferentClientIds() {
        // Arrange
        String clientId1 = "client1";
        String clientId2 = "client2";

        // Act
        CompletableFuture<String> token1Future = jwtProvider.generateToken(clientId1);
        CompletableFuture<String> token2Future = jwtProvider.generateToken(clientId2);
        
        String token1 = token1Future.join();
        String token2 = token2Future.join();

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2, "Tokens for different client IDs should be different");

        // Verify each token contains the correct client ID
        Claims claims1 = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token1)
            .getBody();

        Claims claims2 = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token2)
            .getBody();

        assertEquals(clientId1, claims1.getSubject());
        assertEquals(clientId2, claims2.getSubject());
    }

    @Test
    public void testGenerateTokenMultipleCallsSameClientId() {
        // Arrange
        String clientId = "sameClient";

        // Act
        CompletableFuture<String> token1Future = jwtProvider.generateToken(clientId);
        // Add small delay to ensure different issued at times
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        CompletableFuture<String> token2Future = jwtProvider.generateToken(clientId);
        
        String token1 = token1Future.join();
        String token2 = token2Future.join();

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2, "Multiple calls should generate different tokens due to different issued at times");

        // Both tokens should have the same client ID
        Claims claims1 = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token1)
            .getBody();

        Claims claims2 = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token2)
            .getBody();

        assertEquals(clientId, claims1.getSubject());
        assertEquals(clientId, claims2.getSubject());
    }

    // Async Behavior Tests

    @Test
    public void testGenerateTokenAsyncCompletion() {
        // Arrange
        String clientId = "asyncTestClient";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join(); // Wait for completion

        // Assert
        assertTrue(tokenFuture.isDone());
        assertFalse(tokenFuture.isCompletedExceptionally());
        assertNotNull(token);
    }

    @Test
    public void testGenerateTokenChaining() {
        // Arrange
        String clientId = "chainingTestClient";

        // Act
        CompletableFuture<Integer> tokenLengthFuture = jwtProvider.generateToken(clientId)
            .thenApply(String::length);

        Integer tokenLength = tokenLengthFuture.join();

        // Assert
        assertNotNull(tokenLength);
        assertTrue(tokenLength > 0);
        // JWT tokens are typically quite long
        assertTrue(tokenLength > 100, "JWT token should be reasonably long");
    }

    // Edge Cases
    @Test
    public void testGenerateTokenWithSpecialCharacters() {
        // Arrange
        String clientId = "client@#$%^&*()_+-=[]{}|;':\",./<>?";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert
        assertNotNull(token);
        
        // Verify the client ID is correctly embedded
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        assertEquals(clientId, claims.getSubject());
    }

    @Test
    public void testGenerateTokenWithUnicodeCharacters() {
        // Arrange
        String clientId = "客户端123ñáéíóú🚀";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert
        assertNotNull(token);
        
        // Verify the client ID is correctly embedded
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        assertEquals(clientId, claims.getSubject());
    }

    @Test
    public void testGenerateTokenWithWhitespace() {
        // Arrange
        String clientId = "  client with spaces  ";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert
        assertNotNull(token);
        
        // Verify the client ID is correctly embedded (including whitespace)
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        assertEquals(clientId, claims.getSubject());
    }

    @Test
    public void testGenerateTokenWithLongClientId() {
        // Arrange
        StringBuilder longClientIdBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longClientIdBuilder.append("longclient");
        }
        String longClientId = longClientIdBuilder.toString();

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(longClientId);
        String token = tokenFuture.join();

        // Assert
        assertNotNull(token);
        
        // Verify the long client ID is correctly embedded
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();

        assertEquals(longClientId, claims.getSubject());
    }

    // Concurrency Tests
    @Test
    public void testConcurrentTokenGeneration() throws InterruptedException {
        // Test that multiple concurrent calls work correctly
        String baseClientId = "concurrentClient";
        int threadCount = 10;
        
        Thread[] threads = new Thread[threadCount];
        @SuppressWarnings("unchecked")
        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];
        
        // Create threads that generate tokens concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                futures[index] = jwtProvider.generateToken(baseClientId + index);
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
        
        // Assert all tokens were generated correctly
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(futures[i]);
            String token = futures[i].join();
            assertNotNull(token);
            
            // Verify the token contains the correct client ID
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            assertEquals(baseClientId + i, claims.getSubject());
        }
    }

    // Token Structure Tests
    @Test
    public void testGeneratedTokenStructure() {
        // Arrange
        String clientId = "structureTestClient";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert token structure
        assertNotNull(token);
        assertTrue(token.length() > 0);
        
        // JWT should have exactly 3 parts separated by dots
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have header, payload, and signature");
        
        // Each part should be non-empty
        for (String part : parts) {
            assertFalse(part.isEmpty(), "JWT parts should not be empty");
        }
        
        // Should be valid Base64 URL encoded (no + or / characters, may have padding)
        assertTrue(parts[0].matches("^[A-Za-z0-9_-]*[=]{0,2}$"), "Header should be valid Base64 URL encoded");
        assertTrue(parts[1].matches("^[A-Za-z0-9_-]*[=]{0,2}$"), "Payload should be valid Base64 URL encoded");
        assertTrue(parts[2].matches("^[A-Za-z0-9_-]*[=]{0,2}$"), "Signature should be valid Base64 URL encoded");
    }

    @Test
    public void testTokenSignatureVerification() {
        // Arrange
        String clientId = "signatureTestClient";

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert - Token should be verifiable with the same secret
        assertDoesNotThrow(() -> {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token);
        }, "Token should be verifiable with the correct secret");
    }

    @Test
    public void testTokenSignatureFailsWithWrongSecret() {
        // Arrange
        String clientId = "wrongSecretTestClient";
        // Generate a different secure random secret
        String wrongSecret = generateSecureTestSecret();

        // Act
        CompletableFuture<String> tokenFuture = jwtProvider.generateToken(clientId);
        String token = tokenFuture.join();

        // Assert - Token should NOT be verifiable with wrong secret
        assertThrows(Exception.class, () -> {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token);
        }, "Token should not be verifiable with wrong secret");
    }
}
