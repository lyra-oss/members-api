package edu.lyra.members.api.config.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/**
 * The OpenAPI description springdoc serves at {@code /v3/api-docs}, generated from the controllers and their Bean
 * Validation-annotated request records - the single source of truth the committed Postman collection and the
 * generated k6 client are both generated from, so the API is never defined twice.
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

    /**
     * Several controllers declare a method of the same name (every {@code *Controller} has its own {@code findAll},
     * {@code get}, {@code create}, ...), so springdoc's default operationId (the bare method name) collides across
     * them; it disambiguates by appending {@code _1}, {@code _2}, etc. in whatever order it happens to visit the
     * controllers, which is not guaranteed to stay stable as controllers are added or reordered. Prefixing with the
     * declaring controller's name (e.g. {@code schoolFindAll}, {@code kidGet}) makes every operationId unique and
     * stable by construction instead - both the committed Postman collection and the generated k6 client name their
     * requests/methods after it.
     *
     * @return a customizer that rewrites every operation's id to {@code <controller><Method>}
     */
    @Bean
    OperationCustomizer uniqueOperationIds() {
        return this::withUniqueOperationId;
    }

    private Operation withUniqueOperationId(final Operation operation, final HandlerMethod handlerMethod) {
        // Every @RestController is guaranteed to have a simple name ending in "Controller" - see
        // NamingRulesTest.controllersAreNamedController - so this can strip the suffix unconditionally.
        final String controllerName = handlerMethod.getBeanType().getSimpleName();
        final String entity = controllerName.substring(0, controllerName.length() - "Controller".length());
        final String methodName = handlerMethod.getMethod().getName();
        //@formatter:off
        operation.setOperationId(Character.toLowerCase(entity.charAt(0)) + entity.substring(1) +
                Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1));
        //@formatter:on
        return operation;
    }

}
