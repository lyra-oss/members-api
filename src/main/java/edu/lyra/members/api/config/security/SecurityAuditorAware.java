package edu.lyra.members.api.config.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;

class SecurityAuditorAware
        implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return AuthenticatedPrincipal.currentSubject();
    }

}
