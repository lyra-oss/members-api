package edu.lyra.members.api.config.security;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsJwtSubjectWhenAuthenticatedWithJwt() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Optional<String> auditor = this.auditorAware.getCurrentAuditor();
        assertEquals(Optional.of("user-123"), auditor);
    }

    @Test
    void returnsEmptyWhenSubjectClaimIsAbsent() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .claim("preferred_username", "john")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Optional<String> auditor = this.auditorAware.getCurrentAuditor();
        assertTrue(auditor.isEmpty());
    }

    @Test
    void returnsEmptyWhenNotAuthenticatedWithJwt() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "credentials"));
        final Optional<String> auditor = this.auditorAware.getCurrentAuditor();
        assertTrue(auditor.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoAuthenticationIsPresent() {
        final Optional<String> auditor = this.auditorAware.getCurrentAuditor();
        assertTrue(auditor.isEmpty());
    }

}
