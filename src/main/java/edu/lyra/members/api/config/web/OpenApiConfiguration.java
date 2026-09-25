package edu.lyra.members.api.config.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The OpenAPI description springdoc serves at {@code /v3/api-docs}, generated from the controllers and their Bean
 * Validation-annotated request records - the single source of truth the committed Postman collection is generated
 * from, so the API is never defined twice.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI membersApiOpenApi() {
        //@formatter:off
        return new OpenAPI().info(new Info().title("Lyra Members API")
                                             .description("Service to manage Lyra users")
                                             .version("v0"));
        //@formatter:on
    }

}
