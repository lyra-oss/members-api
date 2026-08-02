package edu.lyra.members.api.config.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import edu.lyra.members.api.config.web.ApiBasePath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static java.util.stream.Collectors.joining;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class EndpointCoverageTest {

    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]+}");

    private static final List<String> ENTITIES   = List.of("parents", "kids", "schools", "teachers", "classrooms");
    private static final List<String> OPERATIONS = List.of("create", "read", "update", "delete");

    @Autowired
    private MockMvc mvc;

    // Actuator also registers its own "controllerEndpointHandlerMapping" of the same bean type; the
    // qualifier picks Spring MVC's own mapping, which is the one holding this app's @RequestMapping
    // controllers.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private ApiBasePath apiBasePath;

    // Every scope the security chain checks for anywhere (see SpringSecurityConfiguration), plus
    // ROLE_admin for PersonController's @PreAuthorize methods, granted all at once: a route is
    // "covered" if this maximally-privileged, authenticated caller can reach it, i.e. it is matched by
    // some rule other than the authority-blind anyRequest().denyAll() fallback. An empty JSON body is
    // sent on every request so that a create/update endpoint's @Valid rejects it with 400 before ever
    // reaching adapter/policy logic — this test cares only about the security layer's verdict, not
    // about what a deeper, unrelated 403 from application code (e.g. a domain policy) might also do.
    private static List<GrantedAuthority> fullPrivileges() {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        for(final String entity : ENTITIES) {
            for(final String operation : OPERATIONS) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + entity + "." + operation));
            }
        }
        authorities.add(new SimpleGrantedAuthority("SCOPE_persons.read"));
        authorities.add(new SimpleGrantedAuthority("ROLE_admin"));
        return authorities;
    }

    @Test
    void everyRegisteredRouteIsMatchedByASpecificSecurityRule()
            throws Exception {
        final List<GrantedAuthority> authorities = fullPrivileges();
        final String                 contextPath = this.apiBasePath.basePath();
        final List<String>           uncovered   = new ArrayList<>();
        for(final RequestMappingInfo info : this.handlerMapping.getHandlerMethods().keySet()) {
            for(final String pattern : info.getPatternValues()) {
                final String servletPath =
                        PATH_VARIABLE.matcher(pattern).replaceAll(UUID.randomUUID().toString());
                for(final RequestMethod method : info.getMethodsCondition().getMethods()) {
                    //@formatter:off
                    final int status = this.mvc.perform(
                            request(HttpMethod.valueOf(method.name()), URI.create(contextPath + servletPath))
                                    .contextPath(contextPath)
                                    .with(jwt().authorities(authorities))
                                    .contentType(APPLICATION_JSON)
                                    .content("{}"))
                            .andReturn().getResponse().getStatus();
                    //@formatter:on
                    if(status == 403) {
                        uncovered.add(method + " " + servletPath);
                    }
                }
            }
        }
        assertTrue(uncovered.isEmpty(),
                   () -> "Routes reachable only through the denyAll fallback (no specific security rule "
                         + "covers them): " + uncovered.stream().sorted().collect(joining(", ")));
    }

    @TestConfiguration
    static class Config {

        @Bean
        JwtDecoder jwtDecoder() {
            return _ -> {
                throw new JwtException("Test JwtDecoder — this test never decodes a real JWT");
            };
        }

    }

}
