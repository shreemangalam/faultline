package dev.shreemangalam.faultline.restore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiDocumentationConfiguration {
    static final String SECURITY_SCHEME = "operatorApiKey";

    @Bean
    OpenAPI restoreOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Faultline Restore API").version("v1")
                        .description("Dependency-aware event recovery API. Mutating operations require an operator API key."))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER)
                                .name(ApiKeyAuthenticationFilter.HEADER)));
    }

    @Bean
    OpenApiCustomizer mutatingOperationSecurity() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperationsMap().forEach((method, operation) -> {
            if (method != io.swagger.v3.oas.models.PathItem.HttpMethod.GET) {
                operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
                operation.addExtension("x-faultline-role", "operator");
            }
        }));
    }
}
