package com.sagittec.lyra.members.api.repositories;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.Validator;

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
class SpringDataRestConfiguration {

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer(Validator validator) {
        return new RepositoryRestConfigurer() {

            @Override
            public void configureValidatingRepositoryEventListener(final ValidatingRepositoryEventListener validatingListener) {
                validatingListener.addValidator("beforeCreate", validator);
            }
        };
    }

}
