package nl.mijnummah.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Metadata of the generated OpenAPI document. The document itself is derived from the controllers by springdoc;
 * it is served only when {@code mijnummah.api-docs.enabled=true} (see application.yml).
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI mijnUmmahOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mijn Ummah API")
                        .version("v1")
                        .description("""
                                API voor de moskee-app en het beheerportaal.
                                Publieke endpoints (/api/v1/public/**) tonen alleen gepubliceerde content van actieve \
                                moskeeën. Beheerendpoints (/api/v1/admin/mosques/{mosqueId}/**) controleren server-side \
                                of de gebruiker rechten heeft voor precies die moskee. Fouten hebben altijd de vorm \
                                {timestamp, status, code, message, path, fieldErrors?}; clients reageren op `code`."""))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
