package com.github.bestheroz.standard.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig {
    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        io.swagger.v3.oas.models.security
                            .SecurityScheme()
                            .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    ),
            )

    @Bean
    fun customizeOperation(): OperationCustomizer =
        OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
            val securityRequirement = handlerMethod.getMethodAnnotation(SecurityRequirement::class.java)
            if (securityRequirement == null) {
                operation.security = null
            }
            operation
        }
}
