package edu.lyra.members.api.config.security;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static java.util.Collections.emptyList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityProviderRoleStrategyResolverTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "RS256").subject("user-123").build();

    @Mock
    private IdentityProviderRoleStrategy unsupportingStrategy;

    @Mock
    private IdentityProviderRoleStrategy supportingStrategy;

    @Test
    void delegatesToTheFirstStrategyThatSupportsTheToken() {
        final String roleAdmin = "ROLE_admin";
        when(this.unsupportingStrategy.supports(any())).thenReturn(false);
        when(this.supportingStrategy.supports(any())).thenReturn(true);
        when(this.supportingStrategy.convertRoles(JWT)).thenReturn(List.of(new SimpleGrantedAuthority(roleAdmin)));
        final IdentityProviderRoleStrategyResolver resolver =
                new IdentityProviderRoleStrategyResolver(List.of(this.unsupportingStrategy, this.supportingStrategy));
        final Collection<GrantedAuthority> authorities = resolver.convert(JWT);
        assertEquals(List.of(new SimpleGrantedAuthority(roleAdmin)), authorities);
    }

    @Test
    void returnsNoAuthoritiesWhenNoStrategySupportsTheToken() {
        when(this.unsupportingStrategy.supports(any())).thenReturn(false);
        final IdentityProviderRoleStrategyResolver resolver =
                new IdentityProviderRoleStrategyResolver(List.of(this.unsupportingStrategy));
        assertEquals(emptyList(), resolver.convert(JWT));
    }

}
