package edu.lyra.members.api.handlers;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@UtilityClass
class AuthenticatedPrincipal {

    boolean hasRole(final String role) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(
                granted -> Objects.equals(granted.getAuthority(), "ROLE_" + role));
    }

    Optional<UUID> currentId() {
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
