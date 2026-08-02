package edu.lyra.members.api.config.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import edu.lyra.members.api.config.CrudResourceNames;
import edu.lyra.members.api.config.web.ApiBasePath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static java.util.stream.Collectors.joining;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestJwtDecoderConfiguration.class)
class EndpointCoverageTest {

    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]+}");

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

    private record Route(RequestMethod method, String path) {

        private String describe() {
            return this.method + " " + this.path;
        }

    }

    private static List<GrantedAuthority> fullPrivileges() {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        for(final String entity : CrudResourceNames.ALL) {
            for(final String operation : OPERATIONS) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + entity + "." + operation));
            }
        }
        authorities.add(new SimpleGrantedAuthority("SCOPE_persons.read"));
        authorities.add(new SimpleGrantedAuthority("ROLE_admin"));
        return authorities;
    }

    // Every distinct (method, path) registered with Spring MVC, path variables replaced by a random
    // UUID segment so each route can be dispatched to a concrete URI.
    private List<Route> registeredRoutes() {
        //@formatter:off
        return this.handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()
                        .map(pattern -> PATH_VARIABLE.matcher(pattern).replaceAll(UUID.randomUUID().toString()))
                        .flatMap(path -> info.getMethodsCondition().getMethods().stream()
                                .map(method -> new Route(method, path))))
                .toList();
        //@formatter:on
    }

    // Every scope the security chain checks for anywhere (see SpringSecurityConfiguration), plus
    // ROLE_admin for PersonController's @PreAuthorize methods, granted all at once: a route is
    // "covered" if this maximally-privileged, authenticated caller can reach it, i.e. it is matched by
    // some rule other than the authority-blind anyRequest().denyAll() fallback. An empty JSON body is
    // sent on every request so that a create/update endpoint's @Valid rejects it with 400 before ever
    // reaching adapter/policy logic. This test only cares about the security layer's verdict: a route
    // whose adapter/policy independently denies with 403 for an unrelated reason (none does today for
    // a GET or an empty-bodied write - see e.g. KidPolicy, which only gates update/delete) would be
    // misreported here as "not covered" rather than "denied for a different reason".
    @Test
    void everyRegisteredRouteIsMatchedByASpecificSecurityRule()
            throws Exception {
        final List<GrantedAuthority> authorities = fullPrivileges();
        final String                 contextPath = this.apiBasePath.basePath();
        final List<String>           uncovered   = new ArrayList<>();
        for(final Route route : this.registeredRoutes()) {
            //@formatter:off
            final int status = this.mvc.perform(
                    request(HttpMethod.valueOf(route.method().name()), URI.create(contextPath + route.path()))
                            .contextPath(contextPath)
                            .with(jwt().authorities(authorities))
                            .contentType(APPLICATION_JSON)
                            .content("{}"))
                    .andReturn().getResponse().getStatus();
            //@formatter:on
            if(status == HttpStatus.FORBIDDEN.value()) {
                uncovered.add(route.describe());
            }
        }
        assertTrue(uncovered.isEmpty(),
                   () -> "Routes reachable only through the denyAll fallback (no specific security rule "
                         + "covers them; a domain policy denial could in principle also produce this "
                         + "signal, but none does today): " + uncovered.stream().sorted().collect(joining(", ")));
    }

}
