package edu.lyra.members.api.cucumber;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@UtilityClass
public class TestSecurityContext {

    public <T> T runAuthenticated(final Supplier<T> action) {
        return runAuthenticated(UUID.randomUUID(), action);
    }

    public <T> T runAuthenticated(final UUID subject, final Supplier<T> action) {
        //@formatter:off
        final Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "none")
                           .subject(subject.toString())
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
