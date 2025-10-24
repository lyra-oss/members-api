package com.sagittec.lyra.members.api.controllers;

import com.sagittec.lyra.members.api.repositories.jpa.Kid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
class SpringDataRestConfiguration {

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer(Validator validator) {
        return new RepositoryRestConfigurer() {

            @Override
            public void configureValidatingRepositoryEventListener(final ValidatingRepositoryEventListener validatingListener) {
                validatingListener.addValidator("beforeCreate", validator);
            }

            @Override
            public void configureRepositoryRestConfiguration(
                    final RepositoryRestConfiguration config,
                    final CorsRegistry cors
            ) {
                config.getExposureConfiguration().forDomainType(Kid.class)
                      .withAssociationExposure((metadata, httpMethods) -> httpMethods.disable(POST, PUT, PATCH));
            }
        };
    }

}
