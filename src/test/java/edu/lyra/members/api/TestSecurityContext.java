package edu.lyra.members.api;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class TestSecurityContext {

    private TestSecurityContext() {
    }

    static <T> T runAuthenticated(final Supplier<T> action) {
        final Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "none")
                           .subject(UUID.randomUUID().toString())
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

}
