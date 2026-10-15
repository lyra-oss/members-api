package edu.lyra.members.api.config.security;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers native-image reflection access for {@code io.swagger.core.v3:swagger-annotations}'s
 * {@link SecurityRequirement}, read off every {@code ..rest} controller method at request time by
 * {@link SecurityRequirementAuthorizationManager}. Unlike the hand-written {@code RequiredAccess} annotation it
 * replaced, this is a third-party type this codebase doesn't own the source of, so it can't simply be marked
 * {@code @Reflective} - this registrar is the manual equivalent.
 *
 * <p>{@code HandlerMethod#getMethodAnnotation} ultimately invokes {@code scopes()}/{@code name()} on the
 * annotation's JDK dynamic proxy; left unregistered, that fails under native-image, since nothing else in this
 * application's boot path otherwise needs this annotation's attributes at runtime - springdoc's own OpenAPI
 * generation never runs in a shipped image, since {@code springdoc.api-docs.enabled} stays {@code false} in every
 * environment (see {@code OpenApiConfiguration}).
 *
 * @author Esteban Cristóbal Rodríguez
 */
class SecurityRequirementRuntimeHints
        implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection().registerType(SecurityRequirement.class, MemberCategory.INVOKE_DECLARED_METHODS);
    }

}
