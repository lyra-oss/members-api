package edu.lyra.members.api.config.security;

import java.util.Arrays;
import java.util.function.Supplier;

import edu.lyra.members.api.exceptions.HandlerLookupException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.function.ThrowingSupplier;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Resolves the {@link org.springframework.web.method.HandlerMethod HandlerMethod} a request would be dispatched to
 * and grants access only when the authenticated principal satisfies that method's {@link RequiredAccess} — the
 * single point where every endpoint's access rule is enforced, in place of a request-matcher list that would
 * otherwise have to be kept in lockstep with the controllers by hand.
 *
 * <p>A request whose path matches no handler, or matches one only under a different HTTP method, is let through here
 * (subject to a genuine, non-anonymous authentication) so Spring MVC's own dispatch produces the ordinary 404 or 405
 * — the same routes that are absent today stay absent, without this class hard-coding which ones they are.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@RequiredArgsConstructor
class RequiredAccessAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String SCOPE_AUTHORITY_PREFIX = "SCOPE_";
    private static final String ROLE_AUTHORITY_PREFIX   = "ROLE_";

    private final RequestMappingHandlerMapping handlerMapping;
    private final AuthenticationTrustResolver  trustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public AuthorizationDecision authorize(
            final Supplier<? extends Authentication> authentication,
            final RequestAuthorizationContext context
    ) {
        final Authentication principal = authentication.get();
        if(! this.isGenuinelyAuthenticated(principal)) {
            return new AuthorizationDecision(false);
        }
        final RequiredAccess required = this.resolveRequiredAccess(context.getRequest());
        if(required == null) {
            return new AuthorizationDecision(true);
        }
        //@formatter:off
        final boolean hasEveryScope =
                Arrays.stream(required.scopes())
                      .allMatch(scope -> this.hasAuthority(principal, SCOPE_AUTHORITY_PREFIX + scope));
        final boolean hasAnyRole = required.roles().length == 0 ||
                                    Arrays.stream(required.roles())
                                          .anyMatch(role -> this.hasAuthority(principal, ROLE_AUTHORITY_PREFIX + role));
        //@formatter:on
        return new AuthorizationDecision(hasEveryScope && hasAnyRole);
    }

    private boolean isGenuinelyAuthenticated(final Authentication authentication) {
        if(authentication == null || ! authentication.isAuthenticated()) {
            return false;
        }
        return ! this.trustResolver.isAnonymous(authentication);
    }

    /**
     * Looks up the handler Spring MVC would dispatch this request to, and returns its {@link RequiredAccess}.
     *
     * @param request the current request
     *
     * @return the matched method's {@link RequiredAccess}, or {@code null} when no handler method matches this exact
     * (HTTP method, path) pair — either because the path is unmapped, or because it is mapped only for a different
     * HTTP method
     */
    private RequiredAccess resolveRequiredAccess(final HttpServletRequest request) {
        final HandlerExecutionChain chain;
        try {
            chain = ThrowingSupplier.of(() -> this.handlerMapping.getHandler(request)).get(HandlerLookupException::new);
        } catch(final HandlerLookupException failure) {
            if(failure.getCause() instanceof HttpRequestMethodNotSupportedException) {
                return null;
            }
            throw failure;
        }
        return chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod ?
                handlerMethod.getMethodAnnotation(RequiredAccess.class) : null;
    }

    private boolean hasAuthority(final Authentication authentication, final String authority) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                              .anyMatch(authority::equals);
    }

}
