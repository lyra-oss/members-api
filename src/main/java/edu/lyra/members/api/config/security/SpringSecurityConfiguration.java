package edu.lyra.members.api.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
@EnableMethodSecurity
class SpringSecurityConfiguration {

    private static final String ANY_SEGMENT = "*";
    private static final String ANY_SUBPATH = "**";

    private static final String ENTITY_ACTUATOR   = "actuator";
    private static final String ENTITY_PARENTS    = "parents";
    private static final String ENTITY_KIDS       = "kids";
    private static final String ENTITY_SCHOOLS    = "schools";
    private static final String ENTITY_TEACHERS   = "teachers";
    private static final String ENTITY_CLASSROOMS = "classrooms";
    private static final String ENTITY_PERSONS    = "persons";

    private static final String ACTUATOR_HEALTH     = path(ENTITY_ACTUATOR, "health");
    private static final String ACTUATOR_HEALTH_ANY = path(ENTITY_ACTUATOR, "health", ANY_SUBPATH);
    private static final String ACTUATOR_INFO       = path(ENTITY_ACTUATOR, "info");

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

    private static final String PARENTS_KIDS         =
            path(ENTITY_PARENTS, ANY_SEGMENT, ENTITY_KIDS, ANY_SEGMENT);
    private static final String CLASSROOMS_TUTOR     =
            path(ENTITY_CLASSROOMS, ANY_SEGMENT, "tutor", ANY_SEGMENT);
    private static final String CLASSROOMS_TEACHERS  =
            path(ENTITY_CLASSROOMS, ANY_SEGMENT, ENTITY_TEACHERS, ANY_SEGMENT);
    private static final String CLASSROOMS_KIDS      =
            path(ENTITY_CLASSROOMS, ANY_SEGMENT, ENTITY_KIDS, ANY_SEGMENT);
    private static final String PERSONS_PARENT_ROLE  = path(ENTITY_PERSONS, ANY_SEGMENT, "parent");
    private static final String PERSONS_TEACHER_ROLE = path(ENTITY_PERSONS, ANY_SEGMENT, "teacher");

    private static final String PARENTS_KIDS_READ         = path(ENTITY_PARENTS, ANY_SEGMENT, ENTITY_KIDS);
    private static final String KIDS_PARENT_READ          = path(ENTITY_KIDS, ANY_SEGMENT, "parent");
    private static final String KIDS_CLASSROOM_READ        = path(ENTITY_KIDS, ANY_SEGMENT, "classroom");
    private static final String SCHOOLS_CLASSROOMS_READ    = path(ENTITY_SCHOOLS, ANY_SEGMENT, ENTITY_CLASSROOMS);
    private static final String SCHOOLS_TEACHERS_READ      = path(ENTITY_SCHOOLS, ANY_SEGMENT, ENTITY_TEACHERS);
    private static final String TEACHERS_SCHOOL_READ       = path(ENTITY_TEACHERS, ANY_SEGMENT, "school");
    private static final String CLASSROOMS_SCHOOL_READ     = path(ENTITY_CLASSROOMS, ANY_SEGMENT, "school");
    private static final String CLASSROOMS_TEACHERS_READ   = path(ENTITY_CLASSROOMS, ANY_SEGMENT, ENTITY_TEACHERS);
    private static final String CLASSROOMS_TUTOR_READ      = path(ENTITY_CLASSROOMS, ANY_SEGMENT, "tutor");

    private static final String SCOPE_PREFIX = "SCOPE_";

    private static final String OP_CREATE = "create";
    private static final String OP_UPDATE = "update";
    private static final String OP_DELETE = "delete";
    private static final String OP_READ   = "read";

    @Bean
    SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        //@formatter:off
        return http.authorizeHttpRequests(auth -> auth
                           .dispatcherTypeMatchers(DispatcherType.ERROR)
                                   .permitAll()
                           .requestMatchers(ACTUATOR_HEALTH, ACTUATOR_HEALTH_ANY, ACTUATOR_INFO)
                                   .permitAll()
                           .requestMatchers(POST, PARENTS)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_CREATE))
                           .requestMatchers(POST, KIDS)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_CREATE))
                           .requestMatchers(POST, SCHOOLS)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_CREATE))
                           .requestMatchers(POST, TEACHERS)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_CREATE))
                           .requestMatchers(POST, CLASSROOMS)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_CREATE))
                           .requestMatchers(PATCH, PARENTS_ANY)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_UPDATE))
                           .requestMatchers(PATCH, KIDS_ANY)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_UPDATE))
                           .requestMatchers(PATCH, SCHOOLS_ANY)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_UPDATE))
                           .requestMatchers(PATCH, TEACHERS_ANY)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_UPDATE))
                           .requestMatchers(PATCH, CLASSROOMS_ANY)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_UPDATE))
                           .requestMatchers(PUT, CLASSROOMS_TUTOR)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_UPDATE))
                           .requestMatchers(PUT, CLASSROOMS_TEACHERS, CLASSROOMS_KIDS)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_UPDATE))
                           .requestMatchers(PUT, PARENTS_KIDS)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_UPDATE))
                           .requestMatchers(PUT, PERSONS_PARENT_ROLE)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_CREATE))
                           .requestMatchers(DELETE, PERSONS_PARENT_ROLE)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_CREATE))
                           .requestMatchers(PUT, PERSONS_TEACHER_ROLE)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_CREATE))
                           .requestMatchers(DELETE, PERSONS_TEACHER_ROLE)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_CREATE))
                           .requestMatchers(PUT, PARENTS_ANY, KIDS_ANY, SCHOOLS_ANY, TEACHERS_ANY, CLASSROOMS_ANY)
                                   .authenticated()
                           .requestMatchers(POST, PERSONS)
                                   .authenticated()
                           .requestMatchers(POST, KIDS_PARENT_READ, KIDS_CLASSROOM_READ)
                                   .authenticated()
                           .requestMatchers(DELETE, PARENTS_ANY)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_DELETE))
                           .requestMatchers(DELETE, KIDS_ANY)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_DELETE))
                           .requestMatchers(DELETE, SCHOOLS_ANY)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_DELETE))
                           .requestMatchers(DELETE, TEACHERS_ANY)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_DELETE))
                           .requestMatchers(DELETE, CLASSROOMS_ANY)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_DELETE))
                           .requestMatchers(GET, PARENTS_KIDS_READ)
                                   .access(bothScopes(ENTITY_PARENTS, ENTITY_KIDS))
                           .requestMatchers(GET, KIDS_PARENT_READ)
                                   .access(bothScopes(ENTITY_KIDS, ENTITY_PARENTS))
                           .requestMatchers(GET, KIDS_CLASSROOM_READ)
                                   .access(bothScopes(ENTITY_KIDS, ENTITY_CLASSROOMS))
                           .requestMatchers(GET, SCHOOLS_CLASSROOMS_READ)
                                   .access(bothScopes(ENTITY_SCHOOLS, ENTITY_CLASSROOMS))
                           .requestMatchers(GET, SCHOOLS_TEACHERS_READ)
                                   .access(bothScopes(ENTITY_SCHOOLS, ENTITY_TEACHERS))
                           .requestMatchers(GET, TEACHERS_SCHOOL_READ)
                                   .access(bothScopes(ENTITY_TEACHERS, ENTITY_SCHOOLS))
                           .requestMatchers(GET, CLASSROOMS_SCHOOL_READ)
                                   .access(bothScopes(ENTITY_CLASSROOMS, ENTITY_SCHOOLS))
                           .requestMatchers(GET, CLASSROOMS_TEACHERS_READ)
                                   .access(bothScopes(ENTITY_CLASSROOMS, ENTITY_TEACHERS))
                           .requestMatchers(GET, CLASSROOMS_TUTOR_READ)
                                   .access(bothScopes(ENTITY_CLASSROOMS, ENTITY_TEACHERS))
                           .requestMatchers(GET, PARENTS, PARENTS_ANY)
                                   .hasAuthority(scope(ENTITY_PARENTS, OP_READ))
                           .requestMatchers(GET, KIDS, KIDS_ANY)
                                   .hasAuthority(scope(ENTITY_KIDS, OP_READ))
                           .requestMatchers(GET, SCHOOLS, SCHOOLS_ANY)
                                   .hasAuthority(scope(ENTITY_SCHOOLS, OP_READ))
                           .requestMatchers(GET, TEACHERS, TEACHERS_ANY)
                                   .hasAuthority(scope(ENTITY_TEACHERS, OP_READ))
                           .requestMatchers(GET, CLASSROOMS, CLASSROOMS_ANY)
                                   .hasAuthority(scope(ENTITY_CLASSROOMS, OP_READ))
                           .requestMatchers(GET, PERSONS, PERSONS_ANY)
                                   .hasAuthority(scope(ENTITY_PERSONS, OP_READ))
                           .requestMatchers(GET, "/")
                                   .authenticated()
                           .anyRequest()
                                   .denyAll())
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

    private static AuthorizationManager<RequestAuthorizationContext> bothScopes(
            final String entityA,
            final String entityB
    ) {
        return AuthorizationManagers.allOf(AuthorityAuthorizationManager.hasAuthority(scope(entityA, OP_READ)),
                                           AuthorityAuthorizationManager.hasAuthority(scope(entityB, OP_READ)));
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
