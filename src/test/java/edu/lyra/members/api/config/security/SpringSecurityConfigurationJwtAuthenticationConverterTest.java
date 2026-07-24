package edu.lyra.members.api.config.security;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringSecurityConfigurationJwtAuthenticationConverterTest {

    @Mock
    private IdentityProviderRoleStrategy roleStrategy;

    @Test
    void mergesScopeAuthoritiesWithRoleStrategyAuthorities() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "none")
                           .claim("scope", "parents.read")
                           .subject("user-123")
                           .build();
        //@formatter:on
        when(this.roleStrategy.supports(jwt)).thenReturn(true);
        when(this.roleStrategy.convertRoles(jwt)).thenReturn(List.of(new SimpleGrantedAuthority("ROLE_admin")));

        final JwtAuthenticationConverter converter =
                new SpringSecurityConfiguration().jwtAuthenticationConverter(List.of(this.roleStrategy));
        final AbstractAuthenticationToken authentication = converter.convert(jwt);

        //@formatter:off
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                                  .collect(Collectors.toSet()))
                .contains("SCOPE_parents.read", "ROLE_admin");
        //@formatter:on
    }

}
