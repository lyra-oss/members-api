package edu.lyra.members.api.config.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import edu.lyra.members.api.config.CrudResourceNames;
import edu.lyra.members.api.config.web.ApiBasePath;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

// @TestInstance(PER_CLASS) lets registeredRoutes() below be a non-static @MethodSource: JUnit reuses
// the one Spring-managed instance (handlerMapping already autowired) instead of needing a static
// method that can't reach it.
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestJwtDecoderConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

        @Override
        public String toString() {
            return this.method + " " + this.path;
        }

    }

    // Every scope the security chain checks for anywhere (see SpringSecurityConfiguration), plus
    // ROLE_admin for PersonController's @PreAuthorize methods, granted all at once.
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
    private Stream<Route> registeredRoutes() {
        //@formatter:off
        return this.handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()
                        .map(pattern -> PATH_VARIABLE.matcher(pattern).replaceAll(UUID.randomUUID().toString()))
                        .flatMap(path -> info.getMethodsCondition().getMethods().stream()
                                .map(method -> new Route(method, path))));
        //@formatter:on
    }

    // A route is "covered" if this maximally-privileged, authenticated caller can reach it, i.e. it is
    // matched by some rule other than the authority-blind anyRequest().denyAll() fallback. An empty
    // JSON body is sent so a create/update endpoint's @Valid rejects it with 400 before ever reaching
    // adapter/policy logic. This assertion only cares about the security layer's verdict: a route whose
    // adapter/policy independently denies with 403 for an unrelated reason (none does today for a GET
    // or an empty-bodied write - see e.g. KidPolicy, which only gates update/delete) would be
    // misreported here as "not covered" rather than "denied for a different reason". One invocation per
    // route, rather than one loop over all of them, keeps each test's coverage to a single route's
    // dispatch path instead of the whole route table at once.
    @ParameterizedTest(name = "{0}")
    @MethodSource("registeredRoutes")
    void everyRegisteredRouteIsMatchedByASpecificSecurityRule(final Route route)
            throws Exception {
        final String contextPath = this.apiBasePath.basePath();
        //@formatter:off
        final int status = this.mvc.perform(
                request(HttpMethod.valueOf(route.method().name()), URI.create(contextPath + route.path()))
                        .contextPath(contextPath)
                        .with(jwt().authorities(fullPrivileges()))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        //@formatter:on
        assertNotEquals(HttpStatus.FORBIDDEN.value(), status,
                         () -> route + " is reachable only through the denyAll fallback (no specific "
                               + "security rule covers it; a domain policy denial could in principle "
                               + "also produce this signal, but none does today)");
    }

}
