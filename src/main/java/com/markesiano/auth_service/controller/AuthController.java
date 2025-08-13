package com.markesiano.auth_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markesiano.auth_service.application.usecases.interfaces.AuthenticateClientUseCase;
import com.markesiano.auth_service.infraestructure.dtos.AuthRequest;
import com.markesiano.auth_service.infraestructure.dtos.AuthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Authentication Service v1", 
    description = "API REST para autenticación de clientes versión 1.0. Proporciona endpoints para la autenticación " +
                 "mediante clientId y productKey, generando tokens JWT válidos para el acceso a recursos protegidos. " +
                 "Implementa validaciones de entrada robustas y manejo de errores estructurado siguiendo las mejores " +
                 "prácticas de diseño de APIs REST y códigos de estado HTTP apropiados."
)
public class AuthController {
    

    private final AuthenticateClientUseCase<AuthResponse, AuthRequest> authenticationClientUseCase;

    public AuthController(AuthenticateClientUseCase<AuthResponse, AuthRequest> authenticationClientUseCase) {
        this.authenticationClientUseCase = authenticationClientUseCase;
    }

    @Operation(
        summary = "Autenticar cliente y generar token JWT",
        description = "Autentica un cliente utilizando su clientId y productKey, retornando un token JWT válido " +
                     "en caso de éxito. El token generado debe ser incluido en el header Authorization con el " +
                     "formato 'Bearer {token}' para acceder a recursos protegidos. Implementa validaciones " +
                     "exhaustivas de entrada y manejo de errores estructurado.",
        tags = {"Authentication Service v1"},
        operationId = "authenticateClient"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticación exitosa - Token JWT generado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "successful-auth-example",
                    description = "Respuesta exitosa de autenticación",
                    value = """
                    {
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRJZCI6ImNsaWVudDEyMyIsInByb2R1Y3RLZXkiOiJwcm9kdWN0XzQ1NiIsImlhdCI6MTYzMjE1MjQwMCwiZXhwIjoxNjMyMjM4ODAwfQ.signature",
                      "error": null,
                      "message": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Solicitud inválida - Datos de entrada incorrectos o faltantes",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = {
                    @ExampleObject(
                        name = "null-request-example",
                        description = "Cuerpo de solicitud nulo",
                        value = """
                        {
                          "token": null,
                          "error": "Bad request",
                          "message": "Request body cannot be null"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "empty-client-id-example",
                        description = "Client ID vacío o nulo",
                        value = """
                        {
                          "token": null,
                          "error": "Bad request",
                          "message": "Client ID cannot be null or empty"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "empty-product-key-example",
                        description = "Product Key vacío o nulo",
                        value = """
                        {
                          "token": null,
                          "error": "Bad request",
                          "message": "Product Key cannot be null or empty"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Autenticación fallida - Credenciales inválidas",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "auth-failed-example",
                    description = "Credenciales inválidas",
                    value = """
                    {
                      "token": null,
                      "error": "Authentication failed",
                      "message": "Invalid client credentials or product key"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor durante el proceso de autenticación",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "server-error-example",
                    value = """
                    {
                      "token": null,
                      "error": "Authentication failed",
                      "message": "Internal server error during authentication process"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Credenciales del cliente para autenticación. Debe incluir un clientId válido " +
                         "y la clave de producto correspondiente (productKey). Ambos campos son obligatorios " +
                         "y no pueden estar vacíos.",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthRequest.class),
                examples = {
                    @ExampleObject(
                        name = "valid-credentials",
                        description = "Ejemplo de credenciales válidas",
                        value = """
                        {
                          "clientId": "client123",
                          "productKey": "product_456_key"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "enterprise-client",
                        description = "Cliente empresarial",
                        value = """
                        {
                          "clientId": "enterprise_corp_001",
                          "productKey": "ENT_PROD_789_XYZ"
                        }
                        """
                    )
                }
            )
        )
        @RequestBody AuthRequest request
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().body(
                AuthResponse.error("Bad request", "Request body cannot be null"));
        }
        if (request.clientId() == null || request.clientId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                AuthResponse.error("Bad request", "Client ID cannot be null or empty"));
        }
        if (request.productKey() == null || request.productKey().isEmpty()) {
            return ResponseEntity.badRequest().body(
                AuthResponse.error("Bad request", "Product Key cannot be null or empty"));
        }
        
        return authenticationClientUseCase.authenticate(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> ResponseEntity.status(401).body(
                AuthResponse.error("Authentication failed", ex.getCause().getMessage())))
            .join();
    }
}
