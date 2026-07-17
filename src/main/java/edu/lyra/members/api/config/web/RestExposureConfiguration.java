package edu.lyra.members.api.config.web;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
class RestExposureConfiguration
        implements RepositoryRestConfigurer {

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

}
