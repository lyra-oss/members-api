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
 */
@UtilityClass
public class AuthenticatedPrincipal {

    public boolean hasRole(final String role) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(
                granted -> Objects.equals(granted.getAuthority(), "ROLE_" + role));
    }

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

    /**
     * The authenticated principal's id, or an {@link AccessDeniedException} when the request carries no JWT bearing a
     * {@code sub} claim that is a valid {@link UUID} — the precondition every self-service write in the vertical slices
     * depends on.
     */
    public UUID requireCurrentId() {
        return currentId().orElseThrow(
                () -> new AccessDeniedException("JWT authentication with a valid \"sub\" claim is required"));
    }

}
