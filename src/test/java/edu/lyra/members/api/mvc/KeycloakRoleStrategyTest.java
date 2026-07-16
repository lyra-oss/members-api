package edu.lyra.members.api.mvc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static java.util.Collections.emptyList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakRoleStrategyTest {

    private final KeycloakRoleStrategy converter = new KeycloakRoleStrategy();

    @Test
    void supportsTokensWithARealmAccessClaim() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .claim("realm_access", Map.of("roles", List.of("parent")))
                           .build();
        //@formatter:on
        assertTrue(converter.supports(jwt));
    }

    @Test
    void doesNotSupportTokensWithoutARealmAccessClaim() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .build();
        //@formatter:on
        assertFalse(converter.supports(jwt));
    }

    @Test
    void mapsRealmRolesToRolePrefixedAuthoritiesPreservingCase() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .claim("realm_access", Map.of("roles", List.of("parent", "teacher")))
                           .build();
        //@formatter:on
        final Collection<GrantedAuthority> authorities = converter.convertRoles(jwt);
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(authority -> "ROLE_parent".equals(authority.getAuthority())));
        assertTrue(authorities.stream().anyMatch(authority -> "ROLE_teacher".equals(authority.getAuthority())));
    }

    @Test
    void returnsNoAuthoritiesWhenRealmAccessClaimIsAbsent() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .build();
        //@formatter:on
        assertEquals(emptyList(), converter.convertRoles(jwt));
    }

    @Test
    void returnsNoAuthoritiesWhenRolesAreAbsentFromRealmAccess() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject("user-123")
                           .claim("realm_access", Map.of())
                           .build();
        //@formatter:on
        assertEquals(emptyList(), converter.convertRoles(jwt));
    }

}
