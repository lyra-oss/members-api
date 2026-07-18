package edu.lyra.members.api.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing and binds its {@code auditorAware} reference to the {@link SecurityAuditorAware} that reads the
 * authenticated principal. Auditing records <em>who</em> acted, so both the enablement and the identity source live
 * here under {@code config.security}; the persistence half — repositories, transactions and the audit-column mapping
 * ({@code Auditable}) — stays in {@code config.jpa}. Keeping this in its own {@code @Configuration} avoids mixing the
 * two concerns.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class SecurityAuditingConfiguration {

    @Bean
    AuditorAware<String> auditorAware() {
        return new SecurityAuditorAware();
    }

}
