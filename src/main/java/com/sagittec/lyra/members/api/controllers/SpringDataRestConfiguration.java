package com.sagittec.lyra.members.api.controllers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;

@Configuration
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
