package edu.lyra.members.api.config.security;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedPrincipalTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateWithAuthorities(final String... authorities) {
        final List<SimpleGrantedAuthority> granted =
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "n/a", granted));
    }

    private static void authenticateAsJwt(final UUID subject, final String... authorities) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject.toString()).build();
        final List<SimpleGrantedAuthority> granted =
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, granted));
    }

    @Test
    void hasRoleReturnsFalseWhenUnauthenticated() {
        assertFalse(AuthenticatedPrincipal.hasRole("admin"));
    }

    @Test
    void hasRoleReturnsTrueWhenTheAuthorityIsGranted() {
        authenticateWithAuthorities("ROLE_admin");
        assertTrue(AuthenticatedPrincipal.hasRole("admin"));
    }

    @Test
    void hasRoleReturnsFalseWhenTheAuthorityIsNotGranted() {
        authenticateWithAuthorities("ROLE_parent");
        assertFalse(AuthenticatedPrincipal.hasRole("admin"));
    }

    @Test
    void isAdminReturnsTrueForAnAdmin() {
        authenticateWithAuthorities("ROLE_admin");
        assertTrue(AuthenticatedPrincipal.isAdmin());
    }

    @Test
    void isAdminReturnsFalseForANonAdmin() {
        authenticateWithAuthorities("ROLE_parent");
        assertFalse(AuthenticatedPrincipal.isAdmin());
    }

    @Test
    void isSelfReturnsTrueWhenRoleAndIdMatch() {
        final UUID id = UUID.randomUUID();
        authenticateAsJwt(id, "ROLE_parent");
        assertTrue(AuthenticatedPrincipal.isSelf("parent", id));
    }

    @Test
    void isSelfReturnsFalseWhenRoleDoesNotMatch() {
        final UUID id = UUID.randomUUID();
        authenticateAsJwt(id, "ROLE_teacher");
        assertFalse(AuthenticatedPrincipal.isSelf("parent", id));
    }

    @Test
    void isSelfReturnsFalseWhenIdDoesNotMatch() {
        authenticateAsJwt(UUID.randomUUID(), "ROLE_parent");
        assertFalse(AuthenticatedPrincipal.isSelf("parent", UUID.randomUUID()));
    }

    @Test
    void requireCurrentIdReturnsTheSubjectClaim() {
        final UUID id = UUID.randomUUID();
        authenticateAsJwt(id);
        assertEquals(id, AuthenticatedPrincipal.requireCurrentId());
    }

    @Test
    void requireCurrentIdThrowsWhenThereIsNoJwtAuthentication() {
        assertThrows(AccessDeniedException.class, AuthenticatedPrincipal::requireCurrentId);
    }

    @Test
    void currentIdReturnsEmptyWhenTheSubjectClaimIsNotAValidUuid() {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("not-a-uuid").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        assertTrue(AuthenticatedPrincipal.currentId().isEmpty());
    }

    @Test
    void currentIdReturnsEmptyWhenNotAuthenticatedWithAJwt() {
        authenticateWithAuthorities();
        assertTrue(AuthenticatedPrincipal.currentId().isEmpty());
    }

}
