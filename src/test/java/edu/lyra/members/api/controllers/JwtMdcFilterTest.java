package edu.lyra.members.api.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtMdcFilterTest {

    private final JwtMdcFilter filter = new JwtMdcFilter();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void populatesMdcWithAllJwtClaims()
            throws ServletException, IOException {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .claim("preferred_username", "john")
                           .claim("email", "john@example.com")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Map<String, String> capturedMdc = new HashMap<>();
        final FilterChain         chain       = (_, _) -> capturedMdc.putAll(MDC.getCopyOfContextMap());

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertEquals("user-123", capturedMdc.get("user.id"));
        assertEquals("john", capturedMdc.get("user.name"));
        assertEquals("john@example.com", capturedMdc.get("user.email"));
    }

    @Test
    void populatesMdcWithSubjectOnlyWhenOptionalClaimsAbsent()
            throws ServletException, IOException {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Map<String, String> capturedMdc = new HashMap<>();
        final FilterChain         chain       = (_, _) -> capturedMdc.putAll(MDC.getCopyOfContextMap());

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertEquals("user-123", capturedMdc.get("user.id"));
        assertNull(capturedMdc.get("user.name"));
        assertNull(capturedMdc.get("user.email"));
    }

    @Test
    void doesNotSetUserIdWhenSubjectIsAbsent()
            throws ServletException, IOException {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .claim("preferred_username", "john")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Map<String, String> capturedMdc = new HashMap<>();
        final FilterChain chain = (_, _) -> {
            final var context = MDC.getCopyOfContextMap();
            if(context != null) {
                capturedMdc.putAll(context);
            }
        };

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertNull(capturedMdc.get("user.id"));
        assertEquals("john", capturedMdc.get("user.name"));
    }

    @Test
    void clearsMdcAfterRequest()
            throws ServletException, IOException {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .claim("preferred_username", "john")
                           .claim("email", "john@example.com")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (_, _) -> {});

        assertNull(MDC.get("user.id"));
        assertNull(MDC.get("user.name"));
        assertNull(MDC.get("user.email"));
    }

    @Test
    void clearsMdcEvenWhenFilterChainThrows() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .claim("preferred_username", "john")
                           .claim("email", "john@example.com")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertThrows(ServletException.class,
                     () -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (_, _) -> {
                         throw new ServletException("test error");
                     }));

        assertNull(MDC.get("user.id"));
        assertNull(MDC.get("user.name"));
        assertNull(MDC.get("user.email"));
    }

    @Test
    void doesNotPopulateMdcWithoutJwtAuthentication()
            throws ServletException, IOException {
        final Map<String, String> capturedMdc = new HashMap<>();
        final FilterChain chain = (_, _) -> {
            final var context = MDC.getCopyOfContextMap();
            if(context != null) {
                capturedMdc.putAll(context);
            }
        };

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertNull(capturedMdc.get("user.id"));
        assertNull(capturedMdc.get("user.name"));
        assertNull(capturedMdc.get("user.email"));
    }

}
