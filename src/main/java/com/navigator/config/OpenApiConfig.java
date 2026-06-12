package com.navigator.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the JWT bearer scheme to springdoc so Swagger UI shows the
 * "Authorize" button and attaches {@code Authorization: Bearer <token>} to
 * requests.
 *
 * The class-level {@link SecurityRequirement} applies the scheme globally, so
 * every operation is locked by default. The public endpoints (auth, health,
 * swagger) still work without a token at runtime — the lock icon is only a UI
 * hint, not enforcement; real enforcement lives in {@code SecurityConfig}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "H1B Career Navigator API",
                version = "v1",
                description = "Track H1B/H4/EAD deadlines, financial planning, and job applications."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Paste the JWT returned by /api/v1/auth/login or /register. Do not include the word 'Bearer'."
)
public class OpenApiConfig {
}
