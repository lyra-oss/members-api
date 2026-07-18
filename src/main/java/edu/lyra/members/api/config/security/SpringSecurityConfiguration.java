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

    private static final String ENTITY_PARENTS    = "parents";
    private static final String ENTITY_KIDS       = "kids";
    private static final String ENTITY_SCHOOLS    = "schools";
    private static final String ENTITY_TEACHERS   = "teachers";
    private static final String ENTITY_CLASSROOMS = "classrooms";
    private static final String ENTITY_PERSONS    = "persons";

    private static final String OP_CREATE = "create";
    private static final String OP_UPDATE = "update";
    private static final String OP_DELETE = "delete";
    private static final String OP_READ   = "read";

    private static final String ROLE_ADMIN = "admin";

    private static final String ANY_SEGMENT = "*";
    private static final String ANY_SUBPATH = "**";

    private static final String PARENTS    = path(ENTITY_PARENTS);
    private static final String KIDS       = path(ENTITY_KIDS);
    private static final String SCHOOLS    = path(ENTITY_SCHOOLS);
    private static final String TEACHERS   = path(ENTITY_TEACHERS);
    private static final String CLASSROOMS = path(ENTITY_CLASSROOMS);
    private static final String PERSONS    = path(ENTITY_PERSONS);

    private static final String PARENTS_ANY    = path(ENTITY_PARENTS, ANY_SUBPATH);
    private static final String KIDS_ANY       = path(ENTITY_KIDS, ANY_SUBPATH);
    private static final String SCHOOLS_ANY    = path(ENTITY_SCHOOLS, ANY_SUBPATH);
    private static final String TEACHERS_ANY   = path(ENTITY_TEACHERS, ANY_SUBPATH);
    private static final String CLASSROOMS_ANY = path(ENTITY_CLASSROOMS, ANY_SUBPATH);
    private static final String PERSONS_ANY    = path(ENTITY_PERSONS, ANY_SUBPATH);

    private static final String PARENTS_KIDS         = path(ENTITY_PARENTS, ANY_SEGMENT, ENTITY_KIDS);
    private static final String CLASSROOMS_TUTOR     = path(ENTITY_CLASSROOMS, ANY_SEGMENT, "tutor");
    private static final String CLASSROOMS_TEACHERS  = path(ENTITY_CLASSROOMS, ANY_SEGMENT, ENTITY_TEACHERS);
    private static final String CLASSROOMS_KIDS      = path(ENTITY_CLASSROOMS, ANY_SEGMENT, ENTITY_KIDS);
    private static final String PERSONS_PARENT_ROLE  = path(ENTITY_PERSONS, ANY_SEGMENT, "parent");
    private static final String PERSONS_TEACHER_ROLE = path(ENTITY_PERSONS, ANY_SEGMENT, "teacher");

    private static final String SCOPE_PREFIX = "SCOPE_";

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

    private static String path(final String... segments) {
        final StringJoiner joiner = new StringJoiner("/", "/", "");
        for(final String segment : segments) {
            joiner.add(segment);
        }
        return joiner.toString();
    }

}
