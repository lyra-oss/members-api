package edu.lyra.members.api.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private static final String SCOPE_PARENTS_CREATE    = SCOPE_PREFIX + "parents.create";
    private static final String SCOPE_KIDS_CREATE       = SCOPE_PREFIX + "kids.create";
    private static final String SCOPE_SCHOOLS_CREATE    = SCOPE_PREFIX + "schools.create";
    private static final String SCOPE_TEACHERS_CREATE   = SCOPE_PREFIX + "teachers.create";
    private static final String SCOPE_PARENTS_UPDATE    = SCOPE_PREFIX + "parents.update";
    private static final String SCOPE_KIDS_UPDATE       = SCOPE_PREFIX + "kids.update";
    private static final String SCOPE_SCHOOLS_UPDATE    = SCOPE_PREFIX + "schools.update";
    private static final String SCOPE_TEACHERS_UPDATE   = SCOPE_PREFIX + "teachers.update";
    private static final String SCOPE_CLASSROOMS_UPDATE = SCOPE_PREFIX + "classrooms.update";
    private static final String SCOPE_PARENTS_DELETE    = SCOPE_PREFIX + "parents.delete";
    private static final String SCOPE_KIDS_DELETE       = SCOPE_PREFIX + "kids.delete";
    private static final String SCOPE_SCHOOLS_DELETE    = SCOPE_PREFIX + "schools.delete";
    private static final String SCOPE_TEACHERS_DELETE   = SCOPE_PREFIX + "teachers.delete";
    private static final String SCOPE_CLASSROOMS_DELETE = SCOPE_PREFIX + "classrooms.delete";
    private static final String SCOPE_PARENTS_READ      = SCOPE_PREFIX + "parents.read";
    private static final String SCOPE_KIDS_READ         = SCOPE_PREFIX + "kids.read";
    private static final String SCOPE_SCHOOLS_READ      = SCOPE_PREFIX + "schools.read";
    private static final String SCOPE_TEACHERS_READ     = SCOPE_PREFIX + "teachers.read";
    private static final String SCOPE_CLASSROOMS_READ   = SCOPE_PREFIX + "classrooms.read";

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
                                   .hasAuthority(SCOPE_PARENTS_CREATE)
                           .requestMatchers(POST, base + KIDS)
                                   .hasAuthority(SCOPE_KIDS_CREATE)
                           .requestMatchers(POST, base + SCHOOLS)
                                   .hasAuthority(SCOPE_SCHOOLS_CREATE)
                           .requestMatchers(POST, base + TEACHERS)
                                   .hasAuthority(SCOPE_TEACHERS_CREATE)
                           .requestMatchers(PATCH, base + PARENTS_ANY)
                                   .hasAuthority(SCOPE_PARENTS_UPDATE)
                           .requestMatchers(PATCH, base + KIDS_ANY)
                                   .hasAuthority(SCOPE_KIDS_UPDATE)
                           .requestMatchers(PATCH, base + SCHOOLS_ANY)
                                   .hasAuthority(SCOPE_SCHOOLS_UPDATE)
                           .requestMatchers(PATCH, base + TEACHERS_ANY)
                                   .hasAuthority(SCOPE_TEACHERS_UPDATE)
                           .requestMatchers(PATCH, base + CLASSROOMS_ANY)
                                   .hasAuthority(SCOPE_CLASSROOMS_UPDATE)
                           .requestMatchers(PUT, base + CLASSROOMS_TUTOR)
                                   .hasAuthority(SCOPE_CLASSROOMS_UPDATE)
                           .requestMatchers(POST, base + CLASSROOMS_TEACHERS, base + CLASSROOMS_KIDS)
                                   .hasAuthority(SCOPE_CLASSROOMS_UPDATE)
                           .requestMatchers(POST, base + PARENTS_KIDS)
                                   .hasAuthority(SCOPE_PARENTS_UPDATE)
                           .requestMatchers(PUT, base + PERSONS_PARENT_ROLE)
                                   .hasAuthority(SCOPE_PARENTS_CREATE)
                           .requestMatchers(DELETE, base + PERSONS_PARENT_ROLE)
                                   .hasAuthority(SCOPE_PARENTS_CREATE)
                           .requestMatchers(PUT, base + PERSONS_TEACHER_ROLE)
                                   .hasAuthority(SCOPE_TEACHERS_CREATE)
                           .requestMatchers(DELETE, base + PERSONS_TEACHER_ROLE)
                                   .hasAuthority(SCOPE_TEACHERS_CREATE)
                           .requestMatchers(DELETE, base + PARENTS_ANY)
                                   .hasAuthority(SCOPE_PARENTS_DELETE)
                           .requestMatchers(DELETE, base + KIDS_ANY)
                                   .hasAuthority(SCOPE_KIDS_DELETE)
                           .requestMatchers(DELETE, base + SCHOOLS_ANY)
                                   .hasAuthority(SCOPE_SCHOOLS_DELETE)
                           .requestMatchers(DELETE, base + TEACHERS_ANY)
                                   .hasAuthority(SCOPE_TEACHERS_DELETE)
                           .requestMatchers(DELETE, base + CLASSROOMS_ANY)
                                   .hasAuthority(SCOPE_CLASSROOMS_DELETE)
                           .requestMatchers(GET, base + PARENTS, base + PARENTS_ANY)
                                   .hasAuthority(SCOPE_PARENTS_READ)
                           .requestMatchers(GET, base + KIDS, base + KIDS_ANY)
                                   .hasAuthority(SCOPE_KIDS_READ)
                           .requestMatchers(GET, base + SCHOOLS, base + SCHOOLS_ANY)
                                   .hasAuthority(SCOPE_SCHOOLS_READ)
                           .requestMatchers(GET, base + TEACHERS, base + TEACHERS_ANY)
                                   .hasAuthority(SCOPE_TEACHERS_READ)
                           .requestMatchers(GET, base + CLASSROOMS, base + CLASSROOMS_ANY)
                                   .hasAuthority(SCOPE_CLASSROOMS_READ)
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

}
