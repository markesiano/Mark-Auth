# Guía de Seguridad - Auth Service

## CONFIGURACIÓN IMPORTANTE DE SEGURIDAD

### JWT Secret Configuration

**CRÍTICO**: Nunca uses secretos hardcodeados en producción.

#### Variables de Entorno Requeridas

```bash
# Requerido en producción
export JWT_SECRET="tu-secret-super-seguro-de-al-menos-256-bits"

# Opcional para testing
export JWT_SECRET_TEST="secret-solo-para-pruebas"
```

#### Generación de Secret Seguro

Para generar un secret seguro para JWT:

```bash
# Opción 1: OpenSSL
openssl rand -base64 64

# Opción 2: Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"

# Opción 3: Python
python -c "import secrets; print(secrets.token_urlsafe(64))"
```

### Configuración de Producción

#### application.yml (Producción)
```yaml
spring:
  security:
    jwt:
      secret: ${JWT_SECRET:#{null}}  # DEBE ser proporcionado via variable de entorno
```

#### application-test.yml (Testing)
```yaml
spring:
  security:
    jwt:
      secret: ${JWT_SECRET_TEST:#{null}}  # Solo para testing
```

### Docker Configuration

#### docker-compose.yml
```yaml
version: '3.8'
services:
  auth-service:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - SPRING_PROFILES_ACTIVE=prod
    # No incluir secretos directamente aquí
```

#### .env file (para Docker Compose)
```bash
# .env - NO commitear este archivo
JWT_SECRET=tu-secret-super-seguro-aqui
```

### Comandos de Verificación

# Ejecutar tests
```
./mvnw test
```

#### Verificar en CI/CD
```bash
# En tu pipeline, asegúrate de configurar las variables
export JWT_SECRET="secret-para-ci-cd"
./mvnw clean test
```
