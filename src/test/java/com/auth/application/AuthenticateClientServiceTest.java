package com.auth.application;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.markesiano.auth_service.application.exceptions.InvalidCredentialsException;
import com.markesiano.auth_service.application.interfaces.JwtKey;
import com.markesiano.auth_service.application.interfaces.RepositoryJwt;
import com.markesiano.auth_service.application.interfaces.RepositoryKey;
import com.markesiano.auth_service.application.usecases.AuthenticateClientService;
import com.markesiano.auth_service.infraestructure.dtos.AuthRequest;
import com.markesiano.auth_service.infraestructure.dtos.AuthResponse;

public class AuthenticateClientServiceTest {

    private AuthenticateClientService authenticateClientService;

    @Mock
    private RepositoryKey repository;
    @Mock
    private JwtKey jwtProvider;
    @Mock
    private RepositoryJwt<String> repositoryJwt;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticateClientService = new AuthenticateClientService(repository, jwtProvider, repositoryJwt);
    }

    // The success authentication test should verify that the service correctly authenticates a client 
    // when the credentials of ClientId and ProductKey are valid.
    // and returns a valid JWT token when provided with valid credentials.

    @Test
    public void testAuthenticateSuccessWithoutJwtCached() {
        // Arrange
        AuthRequest request = new AuthRequest("validClientId", "validProductKey");
        String jwtToken = "validJwtToken";
        AuthResponse expectedResponse = new AuthResponse(jwtToken);


        // Mock the repository to return true for valid client credentials
        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(true));

        when(repositoryJwt.getJwtCached(request.clientId()))
            .thenReturn(CompletableFuture.completedFuture(null));

        when(jwtProvider.generateToken(request.clientId()))
            .thenReturn(CompletableFuture.completedFuture("validJwtToken"));

        when(repositoryJwt.saveJwt(request.clientId(), "validJwtToken"))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        AuthResponse actualResponse = futureResponse.join();
        // Assert
        assertNotNull(actualResponse);
        assertNotNull(actualResponse.token());

        assertEquals(actualResponse, expectedResponse);
        
        // Verify interactions
        verify(repository).isValidClient(request.clientId(), request.productKey());
        verify(repositoryJwt).getJwtCached(request.clientId());
        verify(jwtProvider).generateToken(request.clientId());
        verify(repositoryJwt).saveJwt(request.clientId(), "validJwtToken");
    }

    @Test
    public void testAuthenticateSuccessWithJwtCached() {
        // Arrange
        AuthRequest request = new AuthRequest("validClientId", "validProductKey");
        String cachedJwtToken = "cachedJwtToken";
        AuthResponse expectedResponse = new AuthResponse(cachedJwtToken);

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(repositoryJwt.getJwtCached(request.clientId()))
            .thenReturn(CompletableFuture.completedFuture(cachedJwtToken));

        // Act
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        AuthResponse actualResponse = futureResponse.join();

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(cachedJwtToken, actualResponse.token());

        // Verify interactions - should not generate new token or save to cache
        verify(repository).isValidClient(request.clientId(), request.productKey());
        verify(repositoryJwt).getJwtCached(request.clientId());
        verify(jwtProvider, never()).generateToken(anyString());
        verify(repositoryJwt, never()).saveJwt(anyString(), anyString());
    }

    @Test
    public void testAuthenticateFailureInvalidCredentials() {
        // Arrange
        AuthRequest request = new AuthRequest("invalidClientId", "invalidProductKey");

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // Act & Assert
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        
        CompletionException exception = assertThrows(CompletionException.class, 
            () -> futureResponse.join());
        
        assertTrue(exception.getCause() instanceof InvalidCredentialsException);
        assertEquals("Invalid client ID or product key", exception.getCause().getMessage());

        // Verify interactions - should stop after validation
        verify(repository).isValidClient(request.clientId(), request.productKey());
        verify(repositoryJwt, never()).getJwtCached(anyString());
        verify(jwtProvider, never()).generateToken(anyString());
        verify(repositoryJwt, never()).saveJwt(anyString(), anyString());
    }

    @Test
    public void testAuthenticateFailureRepositoryException() {
        // Arrange
        AuthRequest request = new AuthRequest("clientId", "productKey");
        RuntimeException repositoryException = new RuntimeException("Database connection failed");

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.failedFuture(repositoryException));

        // Act & Assert
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        
        CompletionException exception = assertThrows(CompletionException.class, 
            () -> futureResponse.join());
        
        assertEquals(repositoryException, exception.getCause());

        // Verify interactions
        verify(repository).isValidClient(request.clientId(), request.productKey());
        verify(repositoryJwt, never()).getJwtCached(anyString());
    }

    @Test
    public void testAuthenticateFailureJwtCacheException() {
        // Arrange
        AuthRequest request = new AuthRequest("validClientId", "validProductKey");
        RuntimeException cacheException = new RuntimeException("Cache service unavailable");

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(repositoryJwt.getJwtCached(request.clientId()))
            .thenReturn(CompletableFuture.failedFuture(cacheException));

        // Act & Assert
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        
        CompletionException exception = assertThrows(CompletionException.class, 
            () -> futureResponse.join());
        
        assertEquals(cacheException, exception.getCause());

        // Verify interactions
        verify(repository).isValidClient(request.clientId(), request.productKey());
        verify(repositoryJwt).getJwtCached(request.clientId());
        verify(jwtProvider, never()).generateToken(anyString());
    }

    @Test
    public void testAuthenticateFailureJwtGenerationException() {
        // Arrange
        AuthRequest request = new AuthRequest("validClientId", "validProductKey");
        RuntimeException jwtException = new RuntimeException("JWT generation failed");

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(repositoryJwt.getJwtCached(request.clientId()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(jwtProvider.generateToken(request.clientId()))
            .thenReturn(CompletableFuture.failedFuture(jwtException));

        // Act & Assert
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        
        CompletionException exception = assertThrows(CompletionException.class, 
            () -> futureResponse.join());
        
        assertEquals(jwtException, exception.getCause());

        // Verify interactions
        verify(repository).isValidClient(request.clientId(), request.productKey());
        verify(repositoryJwt).getJwtCached(request.clientId());
        verify(jwtProvider).generateToken(request.clientId());
        verify(repositoryJwt, never()).saveJwt(anyString(), anyString());
    }

    @Test
    public void testAuthenticateFailureSaveJwtException() {
        // Arrange
        AuthRequest request = new AuthRequest("validClientId", "validProductKey");
        String jwtToken = "validJwtToken";
        RuntimeException saveException = new RuntimeException("Failed to save JWT to cache");

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(repositoryJwt.getJwtCached(request.clientId()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(jwtProvider.generateToken(request.clientId()))
            .thenReturn(CompletableFuture.completedFuture(jwtToken));
        when(repositoryJwt.saveJwt(request.clientId(), jwtToken))
            .thenReturn(CompletableFuture.failedFuture(saveException));

        // Act & Assert
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        
        CompletionException exception = assertThrows(CompletionException.class, 
            () -> futureResponse.join());
        
        assertEquals(saveException, exception.getCause());

        // Verify interactions
        verify(repository).isValidClient(request.clientId(), request.productKey());
        verify(repositoryJwt).getJwtCached(request.clientId());
        verify(jwtProvider).generateToken(request.clientId());
        verify(repositoryJwt).saveJwt(request.clientId(), jwtToken);
    }

    @Test
    public void testAuthenticateWithNullRequest() {
        // Act & Assert
        assertThrows(NullPointerException.class, 
            () -> authenticateClientService.authenticate(null));
    }

    @Test
    public void testAuthenticateWithNullClientId() {
        // Arrange
        AuthRequest request = new AuthRequest(null, "validProductKey");

        // Act & Assert
        assertThrows(NullPointerException.class, 
            () -> authenticateClientService.authenticate(request));
    }

    @Test
    public void testAuthenticateWithNullProductKey() {
        // Arrange
        AuthRequest request = new AuthRequest("validClientId", null);

        // Act & Assert
        assertThrows(NullPointerException.class, 
            () -> authenticateClientService.authenticate(request));
    }

    @Test
    public void testAuthenticateWithEmptyClientId() {
        // Arrange
        AuthRequest request = new AuthRequest("", "validProductKey");

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // Act & Assert
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        
        CompletionException exception = assertThrows(CompletionException.class, 
            () -> futureResponse.join());
        
        assertTrue(exception.getCause() instanceof InvalidCredentialsException);

        verify(repository).isValidClient("", "validProductKey");
    }

    @Test
    public void testAuthenticateWithEmptyProductKey() {
        // Arrange
        AuthRequest request = new AuthRequest("validClientId", "");

        when(repository.isValidClient(request.clientId(), request.productKey()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // Act & Assert
        CompletableFuture<AuthResponse> futureResponse = authenticateClientService.authenticate(request);
        
        CompletionException exception = assertThrows(CompletionException.class, 
            () -> futureResponse.join());
        
        assertTrue(exception.getCause() instanceof InvalidCredentialsException);

        verify(repository).isValidClient("validClientId", "");
    }
}
