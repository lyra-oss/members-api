package edu.lyra.members.api.controllers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
class SpringSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final RepositoryRestConfiguration restConfiguration
    ) {
        final String base = restConfiguration.getBasePath().toString();
        //@formatter:off
        return http.authorizeHttpRequests(auth -> auth
                           .requestMatchers("/actuator/**").permitAll()
                           .requestMatchers(POST, base + "/parents").hasAuthority("SCOPE_parents.create")
                           .requestMatchers(POST, base + "/kids").hasAuthority("SCOPE_kids.create")
                           .requestMatchers(POST, base + "/schools").hasAuthority("SCOPE_schools.create")
                           .anyRequest().authenticated())
                   .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                   .csrf(csrf -> csrf.ignoringRequestMatchers(base + "/**"))
                   .addFilterAfter(new JwtMdcFilter(), BearerTokenAuthenticationFilter.class)
                   .build();
        //@formatter:on
    }

}
