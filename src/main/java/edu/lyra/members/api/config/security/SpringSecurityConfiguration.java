package edu.lyra.members.api.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
@EnableMethodSecurity
class SpringSecurityConfiguration {

    private static final String ACTUATOR = "/actuator/**";

    private static final String PARENTS    = "/parents";
    private static final String KIDS       = "/kids";
    private static final String SCHOOLS    = "/schools";
    private static final String TEACHERS   = "/teachers";
    private static final String CLASSROOMS = "/classrooms";
    private static final String PERSONS = "/persons";

    private static final String PARENTS_ANY    = PARENTS + "/**";
    private static final String KIDS_ANY       = KIDS + "/**";
    private static final String SCHOOLS_ANY    = SCHOOLS + "/**";
    private static final String TEACHERS_ANY   = TEACHERS + "/**";
    private static final String CLASSROOMS_ANY = CLASSROOMS + "/**";
    private static final String PERSONS_ANY = PERSONS + "/**";

    private static final String PARENTS_KIDS         = PARENTS + "/*/kids";
    private static final String CLASSROOMS_TUTOR     = CLASSROOMS + "/*/tutor";
    private static final String CLASSROOMS_TEACHERS  = CLASSROOMS + "/*/teachers";
    private static final String CLASSROOMS_KIDS      = CLASSROOMS + "/*/kids";
    private static final String PERSONS_PARENT_ROLE  = PERSONS + "/*/parent";
    private static final String PERSONS_TEACHER_ROLE = PERSONS + "/*/teacher";

    private static final String SCOPE_PREFIX = "SCOPE_";

    private static final String ENTITY_PARENTS    = "parents";
    private static final String ENTITY_KIDS       = "kids";
    private static final String ENTITY_SCHOOLS    = "schools";
    private static final String ENTITY_TEACHERS   = "teachers";
    private static final String ENTITY_CLASSROOMS = "classrooms";

    private static final String OP_CREATE = "create";
    private static final String OP_UPDATE = "update";
    private static final String OP_DELETE = "delete";
    private static final String OP_READ   = "read";

    private static final String ROLE_ADMIN = "admin";

    @Bean
    SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final RepositoryRestConfiguration restConfiguration,
            final JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        final String base = restConfiguration.getBasePath().toString();
        //@formatter:off
        return http.authorizeHttpRequests(auth -> auth
                           .requestMatchers(ACTUATOR)
                                   .permitAll()
                           .requestMatchers(POST, base + PARENTS)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_CREATE))
                           .requestMatchers(POST, base + KIDS)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_CREATE))
                           .requestMatchers(POST, base + SCHOOLS)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_CREATE))
                           .requestMatchers(POST, base + TEACHERS)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_CREATE))
                           .requestMatchers(PATCH, base + PARENTS_ANY)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_UPDATE))
                           .requestMatchers(PATCH, base + KIDS_ANY)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_UPDATE))
                           .requestMatchers(PATCH, base + SCHOOLS_ANY)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_UPDATE))
                           .requestMatchers(PATCH, base + TEACHERS_ANY)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_UPDATE))
                           .requestMatchers(PATCH, base + CLASSROOMS_ANY)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_UPDATE))
                           .requestMatchers(PUT, base + CLASSROOMS_TUTOR)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_UPDATE))
                           .requestMatchers(POST, base + CLASSROOMS_TEACHERS, base + CLASSROOMS_KIDS)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_UPDATE))
                           .requestMatchers(POST, base + PARENTS_KIDS)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_UPDATE))
                           .requestMatchers(PUT, base + PERSONS_PARENT_ROLE)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_CREATE))
                           .requestMatchers(DELETE, base + PERSONS_PARENT_ROLE)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_CREATE))
                           .requestMatchers(PUT, base + PERSONS_TEACHER_ROLE)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_CREATE))
                           .requestMatchers(DELETE, base + PERSONS_TEACHER_ROLE)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_CREATE))
                           .requestMatchers(DELETE, base + PARENTS_ANY)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_DELETE))
                           .requestMatchers(DELETE, base + KIDS_ANY)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_DELETE))
                           .requestMatchers(DELETE, base + SCHOOLS_ANY)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_DELETE))
                           .requestMatchers(DELETE, base + TEACHERS_ANY)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_DELETE))
                           .requestMatchers(DELETE, base + CLASSROOMS_ANY)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_DELETE))
                           .requestMatchers(GET, base + PARENTS, base + PARENTS_ANY)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_READ))
                           .requestMatchers(GET, base + KIDS, base + KIDS_ANY)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_READ))
                           .requestMatchers(GET, base + SCHOOLS, base + SCHOOLS_ANY)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_READ))
                           .requestMatchers(GET, base + TEACHERS, base + TEACHERS_ANY)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_READ))
                           .requestMatchers(GET, base + CLASSROOMS, base + CLASSROOMS_ANY)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_READ))
                           .requestMatchers(GET, base + PERSONS, base + PERSONS_ANY)
                                   .hasRole(ROLE_ADMIN)
                           .anyRequest()
                                   .authenticated())
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

    private static String scope(final String entity, final String operation) {
        return new StringJoiner(".", SCOPE_PREFIX, "").add(entity).add(operation).toString();
    }

}
