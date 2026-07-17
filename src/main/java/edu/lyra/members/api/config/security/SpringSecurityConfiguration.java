package edu.lyra.members.api.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
class SpringSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final RepositoryRestConfiguration restConfiguration,
            final JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        final String base = restConfiguration.getBasePath().toString();
        //@formatter:off
        return http.authorizeHttpRequests(auth -> auth
                           .requestMatchers("/actuator/**").permitAll()
                           .requestMatchers(POST, base + "/parents").hasAuthority("SCOPE_parents.create")
                           .requestMatchers(POST, base + "/kids").hasAuthority("SCOPE_kids.create")
                           .requestMatchers(POST, base + "/schools").hasAuthority("SCOPE_schools.create")
                           .requestMatchers(POST, base + "/teachers").hasAuthority("SCOPE_teachers.create")
                           .requestMatchers(PATCH, base + "/parents/**").hasAuthority("SCOPE_parents.update")
                           .requestMatchers(PATCH, base + "/kids/**").hasAuthority("SCOPE_kids.update")
                           .requestMatchers(PATCH, base + "/schools/**").hasAuthority("SCOPE_schools.update")
                           .requestMatchers(PATCH, base + "/teachers/**").hasAuthority("SCOPE_teachers.update")
                           .requestMatchers(PATCH, base + "/classrooms/**").hasAuthority("SCOPE_classrooms.update")
                           .requestMatchers(PUT, base + "/classrooms/*/tutor")
                                   .hasAuthority("SCOPE_classrooms.update")
                           .requestMatchers(POST, base + "/classrooms/*/teachers", base + "/classrooms/*/kids")
                                   .hasAuthority("SCOPE_classrooms.update")
                           .requestMatchers(POST, base + "/parents/*/kids").hasAuthority("SCOPE_parents.update")
                           .requestMatchers(GET, base + "/parents", base + "/parents/**")
                                   .hasAuthority("SCOPE_parents.read")
                           .requestMatchers(GET, base + "/kids", base + "/kids/**")
                                   .hasAuthority("SCOPE_kids.read")
                           .requestMatchers(GET, base + "/schools", base + "/schools/**")
                                   .hasAuthority("SCOPE_schools.read")
                           .requestMatchers(GET, base + "/teachers", base + "/teachers/**")
                                   .hasAuthority("SCOPE_teachers.read")
                           .requestMatchers(GET, base + "/classrooms", base + "/classrooms/**")
                                   .hasAuthority("SCOPE_classrooms.read")
                           .anyRequest().authenticated())
                   .oauth2ResourceServer(oauth2 -> oauth2
                           .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                   .addFilterAfter(new JwtMdcFilter(), BearerTokenAuthenticationFilter.class)
                   .build();
        //@formatter:on
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(final List<IdentityProviderRoleStrategy> roleStrategies) {
        final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        final IdentityProviderRoleStrategyResolver roleAuthoritiesConverter =
                new IdentityProviderRoleStrategyResolver(roleStrategies);
        final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            final Collection<GrantedAuthority> authorities = new ArrayList<>(scopeAuthoritiesConverter.convert(jwt));
            authorities.addAll(roleAuthoritiesConverter.convert(jwt));
            return authorities;
        });
        return converter;
    }

}
