package com.markesiano.auth_service.application.usecases;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.markesiano.auth_service.application.exceptions.InvalidCredentialsException;
import com.markesiano.auth_service.application.interfaces.JwtKey;
import com.markesiano.auth_service.application.interfaces.RepositoryJwt;
import com.markesiano.auth_service.application.interfaces.RepositoryKey;
import com.markesiano.auth_service.application.usecases.interfaces.AuthenticateClientUseCase;
import com.markesiano.auth_service.infraestructure.dtos.AuthRequest;
import com.markesiano.auth_service.infraestructure.dtos.AuthResponse;

@Service
public class AuthenticateClientService implements AuthenticateClientUseCase<AuthResponse, AuthRequest> {
    private final RepositoryKey repository;
    private final JwtKey jwtProvider;
    private final RepositoryJwt<String> repositoryJwt;

    public AuthenticateClientService(RepositoryKey repository, JwtKey jwtProvider, RepositoryJwt<String> repositoryJwt) {
        this.repository = repository;
        this.jwtProvider = jwtProvider;
        this.repositoryJwt = repositoryJwt;
    }

    @Override
    public CompletableFuture<AuthResponse> authenticate(AuthRequest request) {
        return repository.isValidClient(request.clientId(), request.productKey())
            .thenCompose(isValid -> {
                if (!isValid) {
                    return CompletableFuture.failedFuture(
                        new InvalidCredentialsException("Invalid client ID or product key"));
                }
                return repositoryJwt.getJwtCached(request.clientId())
                    .thenCompose(jwt -> {

                        if (jwt != null) {
                            return CompletableFuture.completedFuture(new AuthResponse(jwt));
                        }

                        return jwtProvider.generateToken(request.clientId())
                            .thenCompose(token ->
                                repositoryJwt.saveJwt(request.clientId(), token)
                                    .thenApply(v -> new AuthResponse(token))
                            );
                    });
            });
    }


}
