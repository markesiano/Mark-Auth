package com.markesiano.auth_service.application.usecases.interfaces;

import java.util.concurrent.CompletableFuture;

public interface AuthenticateClientUseCase<TResponse, TRequest> {
    CompletableFuture<TResponse> authenticate(TRequest request);
}
