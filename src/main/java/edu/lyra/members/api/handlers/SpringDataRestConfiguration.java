package edu.lyra.members.api.handlers;

import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import org.jspecify.annotations.NonNull;
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
    RepositoryRestConfigurer repositoryRestConfigurer(final Validator validator) {
        return new RepositoryRestConfigurer() {

            @Override
            public void configureRepositoryRestConfiguration(
                    final @NonNull RepositoryRestConfiguration config,
                    final @NonNull CorsRegistry cors
            ) {
                config.getExposureConfiguration().forDomainType(Kid.class)
                      .withAssociationExposure((_, httpMethods) -> httpMethods.disable(POST, PUT, PATCH));
            }

            @Override
            public void configureValidatingRepositoryEventListener(final @NonNull ValidatingRepositoryEventListener validatingListener) {
                validatingListener.addValidator("beforeCreate", validator);
            }
        };
    }

    @Bean
    KidAuthorizationEventHandler kidAuthorizationEventHandler(final ParentsRepository parentsRepository) {
        return new KidAuthorizationEventHandler(parentsRepository);
    }

    @Bean
    ParentRegistrationHandler parentRegistrationHandler() {
        return new ParentRegistrationHandler();
    }

    @Bean
    TeacherRegistrationHandler teacherRegistrationHandler() {
        return new TeacherRegistrationHandler();
    }

    @Bean
    ClassroomTeacherAssignmentEventHandler classroomTeacherAssignmentEventHandler() {
        return new ClassroomTeacherAssignmentEventHandler();
    }

}
