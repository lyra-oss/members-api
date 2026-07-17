package edu.lyra.members.api.config.web;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;

@Configuration
class ValidationConfiguration
        implements RepositoryRestConfigurer {

    private final Validator validator;

    ValidationConfiguration(final Validator validator) {
        this.validator = validator;
    }

    @Override
    public void configureValidatingRepositoryEventListener(final @NonNull ValidatingRepositoryEventListener validatingListener) {
        validatingListener.addValidator("beforeCreate", this.validator);
        validatingListener.addValidator("beforeSave", this.validator);
    }

}
