package edu.lyra.members.api.config.jpa;

import java.util.Optional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Supplies a fixed auditor so persistence-layer tests can save entities without standing up Spring Security.
 *
 * <p>{@code Auditable#createdBy} is {@code nullable = false}, so auditing has to be active for any insert to succeed.
 * These tests are about fetch plans and pagination, not about who the auditor is.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@TestConfiguration
@EnableJpaAuditing(auditorAwareRef = "fixedAuditorAware")
class FixedAuditorConfiguration {

    @Bean
    AuditorAware<String> fixedAuditorAware() {
        return () -> Optional.of("persistence-test");
    }

}
