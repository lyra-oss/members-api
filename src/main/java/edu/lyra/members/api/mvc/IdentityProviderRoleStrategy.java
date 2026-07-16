package edu.lyra.members.api.mvc;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

interface IdentityProviderRoleStrategy {

    boolean supports(final Jwt jwt);

    Collection<GrantedAuthority> convertRoles(final Jwt jwt);

}
