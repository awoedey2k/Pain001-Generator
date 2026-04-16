package com.lanre.personl.iso20022.api.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ISO 20022 Gateway API",
                version = "1.0",
                description = "Discoverable contract for payment generation, validation, routing, lifecycle, translation, and reconciliation endpoints.",
                contact = @Contact(name = "ISO 20022 Gateway")
        )
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "HTTP Basic authentication for protected /api/v1/** endpoints."
)
public class OpenApiConfiguration {

    @Bean
    OpenAPI iso20022GatewayOpenApi() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("ISO 20022 Gateway API")
                        .version("1.0")
                        .description("Generated OpenAPI contract for the ISO 20022 gateway demo.")
                        .license(new License().name("Internal project documentation")));
    }
}
