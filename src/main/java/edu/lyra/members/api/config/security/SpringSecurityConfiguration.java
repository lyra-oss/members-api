package edu.lyra.members.api.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Wires the OAuth2 resource server and delegates every access decision, endpoint by endpoint, to
 * {@link RequiredAccessAuthorizationManager} reading each controller method's {@link RequiredAccess}. This
 * configuration itself only carries the handful of rules that are not a single {@code ..rest} controller's business:
 * the actuator's health/info probes (infrastructure, permitted without authentication) and the JWT-to-authorities
 * conversion feeding every {@link RequiredAccess} check.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Configuration
class SpringSecurityConfiguration {

    private static final String ACTUATOR_HEALTH     = "/actuator/health";
    private static final String ACTUATOR_HEALTH_ANY  = "/actuator/health/**";
    private static final String ACTUATOR_INFO        = "/actuator/info";

    @Bean
    SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final JwtAuthenticationConverter jwtAuthenticationConverter,
            @Qualifier("requestMappingHandlerMapping") final RequestMappingHandlerMapping handlerMapping
    ) {
        //@formatter:off
        return http.authorizeHttpRequests(auth -> auth
                           .dispatcherTypeMatchers(DispatcherType.ERROR)
                                   .permitAll()
                           .requestMatchers(ACTUATOR_HEALTH, ACTUATOR_HEALTH_ANY, ACTUATOR_INFO)
                                   .permitAll()
                           .anyRequest()
                                   .access(new RequiredAccessAuthorizationManager(handlerMapping)))
                   .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                   .csrf(AbstractHttpConfigurer::disable)
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
