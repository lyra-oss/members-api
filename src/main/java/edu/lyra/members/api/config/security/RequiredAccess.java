package edu.lyra.members.api.config.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;

/**
 * Declares the OAuth2 scopes and, optionally, the realm roles a caller must hold to reach the annotated controller
 * method. This is the single place each endpoint's access rule is written down:
 * {@link RequiredAccessAuthorizationManager} enforces it before Spring MVC dispatches the request, and springdoc
 * reads it to document each operation's security requirements — so the rule never drifts between the security
 * filter, the API documentation and a hand-maintained table.
 *
 * <p>Every {@code scopes} entry is required: its {@code SCOPE_}-prefixed authority must be present in the token. An
 * empty {@code scopes} array requires no scope beyond authentication. When {@code roles} is non-empty, holding any
 * one of them (as a {@code ROLE_}-prefixed authority) is also required; an empty {@code roles} array imposes no role
 * requirement.
 *
 * <p>Every method mapped by {@code @GetMapping}/{@code @PostMapping}/{@code @PutMapping}/{@code @PatchMapping}/
 * {@code @DeleteMapping} in a {@code ..rest} package must carry this annotation, enforced by an ArchUnit rule, so a
 * new endpoint can never go live without an explicit, reviewable access decision.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Reflective
public @interface RequiredAccess {

    /**
     * The OAuth2 scopes the caller's token must all carry, as {@code SCOPE_}-prefixed authorities.
     *
     * @return the required scopes; empty if none are required beyond authentication
     */
    String[] scopes() default {};

    /**
     * The realm roles of which the caller must hold at least one, as {@code ROLE_}-prefixed authorities.
     *
     * @return the accepted roles; empty if no role is required
     */
    String[] roles() default {};

}
