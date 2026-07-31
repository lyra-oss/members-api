package edu.lyra.members.api.config.security;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * The only door through which code outside {@code config} may look at the authenticated principal: vertical slices
 * express their authorization decisions against this facade instead of reaching into
 * {@link SecurityContextHolder}/JWT internals (a boundary enforced by an architecture test).
 *
 * @author Esteban Cristóbal Rodríguez
 */
@UtilityClass
public class AuthenticatedPrincipal {

    /**
     * Checks whether the authenticated principal has been granted the given role.
     *
     * @param role the role to check, without the {@code ROLE_} prefix (e.g. {@code "admin"})
     *
     * @return {@code true} if the current {@link Authentication} carries a {@code ROLE_}-prefixed authority matching
     * {@code role}, {@code false} otherwise (including when there is no authenticated principal)
     */
    public boolean hasRole(final String role) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(
                granted -> Objects.equals(granted.getAuthority(), "ROLE_" + role));
    }

    /**
     * @return {@code true} if the current {@link Authentication} carries the {@code ROLE_admin} authority
     */
    public boolean isAdmin() {
        return AuthenticatedPrincipal.hasRole("admin");
    }

    /**
     * Checks whether the authenticated principal holds the given role and its id matches {@code id} — the
     * "is this the same person as the one holding this role" check every ownership-based authorization
     * decision in the vertical slices boils down to.
     *
     * @param role the role to check, without the {@code ROLE_} prefix (e.g. {@code "parent"})
     * @param id the id to match against the principal's id; may be {@code null} (never matches)
     *
     * @return {@code true} if the principal has {@code role} and its id equals {@code id}
     */
    public boolean isSelf(final String role, final UUID id) {
        return AuthenticatedPrincipal.hasRole(role) &&
               AuthenticatedPrincipal.currentId().map(current -> Objects.equals(current, id)).orElse(false);
    }

    /**
     * Returns the authenticated principal's id, read from the JWT's {@code sub} claim — the precondition every
     * self-service write in the vertical slices depends on.
     *
     * @return the authenticated principal's id
     * @throws AccessDeniedException if the request carries no JWT bearing a {@code sub} claim that is a valid
     *         {@link UUID}
     * @see #currentId()
     */
    public UUID requireCurrentId() {
        return AuthenticatedPrincipal.currentId().orElseThrow(
                () -> new AccessDeniedException("JWT authentication with a valid \"sub\" claim is required"));
    }

    /**
     * Reads the authenticated principal's id from the current JWT's {@code sub} claim, if any.
     *
     * @return the principal's id, or {@link Optional#empty()} when there is no JWT authentication, or its {@code sub}
     * claim is missing or is not a valid {@link UUID}
     */
    public Optional<UUID> currentId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return empty();
        }
        try {
            return ofNullable(jwtAuth.getToken().getSubject()).map(UUID::fromString);
        } catch(final IllegalArgumentException _) {
            return empty();
        }
    }

}
