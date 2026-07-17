package edu.lyra.members.api.config.security;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static java.util.Collections.emptyList;

class IdentityProviderRoleStrategyResolver
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final List<IdentityProviderRoleStrategy> strategies;

    IdentityProviderRoleStrategyResolver(final List<IdentityProviderRoleStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public Collection<GrantedAuthority> convert(final @NonNull Jwt jwt) {
        //@formatter:off
        return this.strategies.stream().filter(strategy -> strategy.supports(jwt))
                              .findFirst()
                              .map(strategy -> strategy.convertRoles(jwt))
                              .orElse(emptyList());
        //@formatter:on
    }

}
