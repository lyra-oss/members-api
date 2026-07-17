package edu.lyra.members.api.config.jpa;

import edu.lyra.members.api.MembersApiApplication;
import edu.lyra.members.api.config.security.SecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackageClasses = MembersApiApplication.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableTransactionManagement
class SpringDataJpaConfiguration {

    @Bean
    AuditorAware<String> auditorAware() {
        return new SecurityAuditorAware();
    }

}