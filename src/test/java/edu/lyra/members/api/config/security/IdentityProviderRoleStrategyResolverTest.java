package edu.lyra.members.api.config.security;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static java.util.Collections.emptyList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityProviderRoleStrategyResolverTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "RS256").subject("user-123").build();

    @Test
    void delegatesToTheFirstStrategyThatSupportsTheToken() {
        final IdentityProviderRoleStrategy unsupportingStrategy = mock(IdentityProviderRoleStrategy.class);
        when(unsupportingStrategy.supports(any())).thenReturn(false);
        final IdentityProviderRoleStrategy supportingStrategy = mock(IdentityProviderRoleStrategy.class);
        when(supportingStrategy.supports(any())).thenReturn(true);
        final String roleAdmin = "ROLE_admin";
        when(supportingStrategy.convertRoles(JWT)).thenReturn(List.of(new SimpleGrantedAuthority(roleAdmin)));
        final IdentityProviderRoleStrategyResolver resolver =
                new IdentityProviderRoleStrategyResolver(List.of(unsupportingStrategy, supportingStrategy));
        final Collection<GrantedAuthority> authorities = resolver.convert(JWT);
        assertEquals(List.of(new SimpleGrantedAuthority(roleAdmin)), authorities);
    }

    @Test
    void returnsNoAuthoritiesWhenNoStrategySupportsTheToken() {
        final IdentityProviderRoleStrategy unsupportingStrategy = mock(IdentityProviderRoleStrategy.class);
        when(unsupportingStrategy.supports(any())).thenReturn(false);
        final IdentityProviderRoleStrategyResolver resolver =
                new IdentityProviderRoleStrategyResolver(List.of(unsupportingStrategy));
        assertEquals(emptyList(), resolver.convert(JWT));
    }

}
