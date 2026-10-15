package edu.lyra.members.api.config.web;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/**
 * The OpenAPI description springdoc serves at {@code /v3/api-docs}, generated from the controllers and their Bean
 * Validation-annotated request records - the single source of truth the committed Postman collection and the generated
 * k6 client are both generated from, so the API is never defined twice.
 *
 * <p>The {@code oauth2} {@link SecurityScheme} declared here is what every controller method's
 * {@code @SecurityRequirement(name = "oauth2", ...)} refers to by name; springdoc resolves the two together when
 * rendering each operation's security section, so this is the one place the full scope catalog - which scope means what
 * - needs to be kept in sync with {@code keycloak/lyra-realm.json}'s {@code clientScopes} (that file isn't shipped with
 * the application artifact, so nothing re-derives this list from it automatically).
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Configuration
//@formatter:off
@SecurityScheme(
        name = "oauth2",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                password = @OAuthFlow(
                        // Illustrative only: the real Keycloak host and realm are environment-specific and supplied
                        // at deploy time (spring.security.oauth2.resourceserver.jwt.issuer-uri has no built-in
                        // default - see SpringSecurityConfiguration); springdoc still requires some syntactically
                        // valid URL here to render the scheme.
                        tokenUrl = "https://placeholder.invalid/realms/lyra/protocol/openid-connect/token",
                        scopes = {
                                @OAuthScope(
                                        name = "classrooms.create",
                                        description = "Permission to create classrooms"),
                                @OAuthScope(
                                        name = "classrooms.delete",
                                        description = "Permission to delete classrooms"),
                                @OAuthScope(
                                        name = "classrooms.read",
                                        description = "Permission to look up classroom records"),
                                @OAuthScope(
                                        name = "classrooms.update",
                                        description = "Permission to update classroom records"),
                                @OAuthScope(
                                        name = "kids.create",
                                        description = "Permission to register kids"),
                                @OAuthScope(
                                        name = "kids.delete",
                                        description = "Permission to delete kid records"),
                                @OAuthScope(
                                        name = "kids.read",
                                        description = "Permission to look up kid records"),
                                @OAuthScope(
                                        name = "kids.update",
                                        description = "Permission to update kid records"),
                                @OAuthScope(
                                        name = "parents.create",
                                        description = "Permission to create parent accounts"),
                                @OAuthScope(
                                        name = "parents.delete",
                                        description = "Permission to delete parent accounts"),
                                @OAuthScope(
                                        name = "parents.read",
                                        description = "Permission to look up parent records"),
                                @OAuthScope(
                                        name = "parents.update",
                                        description = "Permission to update parent accounts"),
                                @OAuthScope(
                                        name = "persons.read",
                                        description = "Permission to look up person records"),
                                @OAuthScope(
                                        name = "schools.create",
                                        description = "Permission to create schools"),
                                @OAuthScope(
                                        name = "schools.delete",
                                        description = "Permission to delete schools"),
                                @OAuthScope(
                                        name = "schools.read",
                                        description = "Permission to look up school records"),
                                @OAuthScope(
                                        name = "schools.update",
                                        description = "Permission to update school records"),
                                @OAuthScope(
                                        name = "teachers.create",
                                        description = "Permission to register teachers"),
                                @OAuthScope(
                                        name = "teachers.delete",
                                        description = "Permission to delete teacher accounts"),
                                @OAuthScope(
                                        name = "teachers.read",
                                        description = "Permission to look up teacher records"),
                                @OAuthScope(
                                        name = "teachers.update",
                                        description = "Permission to update teacher records"),
                                // Not a real Keycloak client scope - see SecurityRequirementAuthorizationManager's
                                // ROLE_SCOPE_PREFIX ("role:") handling, which reads this as a realm-role requirement
                                // instead. OpenAPI's security model has no vocabulary of its own for "the caller's
                                // realm role", so it rides inside the same scopes array as every real scope.
                                @OAuthScope(
                                        name = "role:admin",
                                        description =
                                                "Marker: requires the realm role 'admin', not a real OAuth2 scope")
                        }
                )
        )
)
//@formatter:on
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
