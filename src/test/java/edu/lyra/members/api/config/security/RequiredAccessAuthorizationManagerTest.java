package edu.lyra.members.api.config.security;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import edu.lyra.members.api.exceptions.HandlerLookupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiredAccessAuthorizationManagerTest {

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    private RequiredAccessAuthorizationManager manager;
    private MockHttpServletRequest             request;
    private RequestAuthorizationContext        context;

    @BeforeEach
    void setUp() {
        this.manager = new RequiredAccessAuthorizationManager(this.handlerMapping);
        this.request = new MockHttpServletRequest();
        this.context = new RequestAuthorizationContext(this.request);
    }

    @Test
    void deniesWhenThereIsNoAuthentication() {
        assertFalse(this.manager.authorize(() -> null, this.context).isGranted());
    }

    @Test
    void deniesWhenNotAuthenticated() {
        final TestingAuthenticationToken token = new TestingAuthenticationToken("user", "n/a");
        token.setAuthenticated(false);
        assertFalse(this.manager.authorize(() -> token, this.context).isGranted());
    }

    @Test
    void deniesAnonymousAuthentication() {
        //@formatter:off
        final Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        //@formatter:on
        assertFalse(this.manager.authorize(() -> anonymous, this.context).isGranted());
    }

    @Test
    void grantsAGenuinelyAuthenticatedCallerWhenNoScopeOrRoleIsRequired()
            throws Exception {
        assertTrue(this.decide(this::authenticatedWithNoAuthorities, "openMethod").isGranted());
    }

    private Authentication authenticatedWithNoAuthorities() {
        return new TestingAuthenticationToken("user", "n/a", List.<GrantedAuthority>of());
    }

    @Test
    void grantsWhenNoHandlerMatchesTheRequest()
            throws Exception {
        when(this.handlerMapping.getHandler(this.request)).thenReturn(null);
        assertTrue(this.manager.authorize(this::authenticatedWithNoAuthorities, this.context).isGranted());
    }

    @Test
    void grantsWhenThePathMatchesOnlyUnderAnotherHttpMethod()
            throws Exception {
        when(this.handlerMapping.getHandler(this.request))
                .thenThrow(new HttpRequestMethodNotSupportedException("PUT"));
        assertTrue(this.manager.authorize(this::authenticatedWithNoAuthorities, this.context).isGranted());
    }

    @Test
    void propagatesAnUnexpectedHandlerLookupFailure()
            throws Exception {
        final HttpMediaTypeNotAcceptableException cause = new HttpMediaTypeNotAcceptableException("no match");
        when(this.handlerMapping.getHandler(this.request)).thenThrow(cause);
        //@formatter:off
        final HandlerLookupException thrown = assertThrows(HandlerLookupException.class,
                () -> this.manager.authorize(this::authenticatedWithNoAuthorities, this.context));
        //@formatter:on
        assertEquals(cause, thrown.getCause());
    }

    @Test
    void grantsWhenTheMatchedMethodCarriesNoRequiredAccess()
            throws Exception {
        assertTrue(this.decide(this::authenticatedWithNoAuthorities, "unannotatedMethod").isGranted());
    }

    @Test
    void deniesWhenARequiredScopeIsMissing()
            throws Exception {
        //@formatter:off
        final Authentication authentication =
                new TestingAuthenticationToken("user", "n/a", new SimpleGrantedAuthority("SCOPE_other.scope"));
        //@formatter:on
        assertFalse(this.decide(() -> authentication, "scopedMethod").isGranted());
    }

    @Test
    void grantsWhenTheRequiredScopeIsPresent()
            throws Exception {
        //@formatter:off
        final Authentication authentication =
                new TestingAuthenticationToken("user", "n/a", new SimpleGrantedAuthority("SCOPE_kids.read"));
        //@formatter:on
        assertTrue(this.decide(() -> authentication, "scopedMethod").isGranted());
    }

    @Test
    void deniesWhenOnlySomeOfSeveralRequiredScopesArePresent()
            throws Exception {
        //@formatter:off
        final Authentication authentication =
                new TestingAuthenticationToken("user", "n/a", new SimpleGrantedAuthority("SCOPE_kids.read"));
        //@formatter:on
        assertFalse(this.decide(() -> authentication, "multiScopeMethod").isGranted());
    }

    @Test
    void grantsWhenEveryRequiredScopeIsPresent()
            throws Exception {
        //@formatter:off
        final Authentication authentication = new TestingAuthenticationToken("user", "n/a",
                new SimpleGrantedAuthority("SCOPE_kids.read"), new SimpleGrantedAuthority("SCOPE_classrooms.read"));
        //@formatter:on
        assertTrue(this.decide(() -> authentication, "multiScopeMethod").isGranted());
    }

    @Test
    void deniesWhenTheScopeIsPresentButNoRequiredRoleIs()
            throws Exception {
        //@formatter:off
        final Authentication authentication =
                new TestingAuthenticationToken("user", "n/a", new SimpleGrantedAuthority("SCOPE_parents.create"));
        //@formatter:on
        assertFalse(this.decide(() -> authentication, "scopedAndRoledMethod").isGranted());
    }

    @Test
    void grantsWhenBothTheScopeAndTheRoleArePresent()
            throws Exception {
        //@formatter:off
        final Authentication authentication = new TestingAuthenticationToken("user", "n/a",
                new SimpleGrantedAuthority("SCOPE_parents.create"), new SimpleGrantedAuthority("ROLE_admin"));
        //@formatter:on
        assertTrue(this.decide(() -> authentication, "scopedAndRoledMethod").isGranted());
    }

    @Test
    void grantsWhenAnyOneOfSeveralAcceptedRolesIsPresent()
            throws Exception {
        final Authentication authentication =
                new TestingAuthenticationToken("user", "n/a", new SimpleGrantedAuthority("ROLE_teacher"));
        assertTrue(this.decide(() -> authentication, "multiRoleMethod").isGranted());
    }

    @Test
    void deniesWhenNoneOfSeveralAcceptedRolesIsPresent()
            throws Exception {
        final Authentication authentication =
                new TestingAuthenticationToken("user", "n/a", new SimpleGrantedAuthority("ROLE_parent"));
        assertFalse(this.decide(() -> authentication, "multiRoleMethod").isGranted());
    }

    private AuthorizationDecision decide(final Supplier<Authentication> authentication, final String methodName)
            throws Exception {
        final HandlerExecutionChain chain = new HandlerExecutionChain(handlerMethodFor(methodName));
        when(this.handlerMapping.getHandler(this.request)).thenReturn(chain);
        return this.manager.authorize(authentication, this.context);
    }

    private static HandlerMethod handlerMethodFor(final String methodName) {
        final Fixture fixture = new Fixture();
        try {
            final Method method = Fixture.class.getDeclaredMethod(methodName);
            return new HandlerMethod(fixture, method);
        } catch(final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private static final class Fixture {

        @RequiredAccess
        void openMethod() {}

        void unannotatedMethod() {}

        @RequiredAccess(scopes = "kids.read")
        void scopedMethod() {}

        @RequiredAccess(scopes = {"kids.read", "classrooms.read"})
        void multiScopeMethod() {}

        @RequiredAccess(scopes = "parents.create", roles = "admin")
        void scopedAndRoledMethod() {}

        @RequiredAccess(roles = {"admin", "teacher"})
        void multiRoleMethod() {}

    }

}
