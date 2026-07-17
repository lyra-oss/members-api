package edu.lyra.members.api.config.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Optional.ofNullable;

/**
 * Resolves the auditor stamped onto {@code @CreatedBy}/{@code @LastModifiedBy} columns from the authenticated
 * principal's JWT subject. Auditing is a security concern — it records <em>who</em> acted — so identity resolution
 * lives here, in {@code config.security}, while JPA auditing itself is enabled from {@code config.jpa} against this
 * bean. Public as the cross-package seam consumed by that JPA configuration.
 */
public class SecurityAuditorAware
        implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return Optional.empty();
        }
        return ofNullable(jwtAuth.getToken().getSubject());
    }

}
