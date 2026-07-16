package edu.lyra.members.api.mvc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Component
class KeycloakRoleStrategy
        implements IdentityProviderRoleStrategy {

    @Override
    public boolean supports(final Jwt jwt) {
        return jwt.hasClaim("realm_access");
    }

    @Override
    public Collection<GrantedAuthority> convertRoles(final Jwt jwt) {
        final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if(realmAccess == null) {
            return emptyList();
        }
        @SuppressWarnings("unchecked")
        final List<String> roles = (List<String>) realmAccess.getOrDefault("roles", emptyList());
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_".concat(role))).collect(toList());
    }

}
