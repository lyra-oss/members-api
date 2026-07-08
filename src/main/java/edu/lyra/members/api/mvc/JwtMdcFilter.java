package edu.lyra.members.api.mvc;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import static java.util.Optional.ofNullable;

import static org.slf4j.MDC.put;
import static org.slf4j.MDC.remove;

class JwtMdcFilter
        extends OncePerRequestFilter {

    private static final String USER_ID    = "user.id";
    private static final String USER_NAME  = "user.name";
    private static final String USER_EMAIL = "user.email";

    @Override
    protected void doFilterInternal(
            final @NonNull HttpServletRequest request,
            final @NonNull HttpServletResponse response,
            final FilterChain filterChain
    )
            throws ServletException, IOException {
        try {
            populateMdc();
            filterChain.doFilter(request, response);
        } finally {
            remove(USER_ID);
            remove(USER_NAME);
            remove(USER_EMAIL);
        }
    }

    private static void populateMdc() {
        if(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            final Jwt token = jwtAuth.getToken();
            ofNullable(token.getSubject()).ifPresent(subject -> put(USER_ID, subject));
            ofNullable(token.getClaimAsString("preferred_username")).ifPresent(username -> put(USER_NAME, username));
            ofNullable(token.getClaimAsString("email")).ifPresent(email -> put(USER_EMAIL, email));
        }
    }

}
