package edu.lyra.members.api.handlers;

import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.Teacher;
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
                config.getExposureConfiguration().forDomainType(Parent.class)
                      .withItemExposure((_, httpMethods) -> httpMethods.disable(PUT));
                config.getExposureConfiguration().forDomainType(Kid.class)
                      .withItemExposure((_, httpMethods) -> httpMethods.disable(PUT));
                config.getExposureConfiguration().forDomainType(Teacher.class)
                      .withItemExposure((_, httpMethods) -> httpMethods.disable(PUT));
                config.getExposureConfiguration().forDomainType(School.class)
                      .withItemExposure((_, httpMethods) -> httpMethods.disable(PUT));
                config.getExposureConfiguration().forDomainType(Classroom.class)
                      .withItemExposure((_, httpMethods) -> httpMethods.disable(PUT));
            }

            @Override
            public void configureValidatingRepositoryEventListener(final @NonNull ValidatingRepositoryEventListener validatingListener) {
                validatingListener.addValidator("beforeCreate", validator);
                validatingListener.addValidator("beforeSave", validator);
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

    @Bean
    ParentUpdateAuthorizationEventHandler parentUpdateAuthorizationEventHandler() {
        return new ParentUpdateAuthorizationEventHandler();
    }

    @Bean
    KidUpdateAuthorizationEventHandler kidUpdateAuthorizationEventHandler() {
        return new KidUpdateAuthorizationEventHandler();
    }

    @Bean
    TeacherUpdateAuthorizationEventHandler teacherUpdateAuthorizationEventHandler() {
        return new TeacherUpdateAuthorizationEventHandler();
    }

    @Bean
    SchoolUpdateAuthorizationEventHandler schoolUpdateAuthorizationEventHandler() {
        return new SchoolUpdateAuthorizationEventHandler();
    }

    @Bean
    ClassroomUpdateAuthorizationEventHandler classroomUpdateAuthorizationEventHandler() {
        return new ClassroomUpdateAuthorizationEventHandler();
    }

}
