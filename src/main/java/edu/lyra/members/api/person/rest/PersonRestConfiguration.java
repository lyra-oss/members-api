package edu.lyra.members.api.person.rest;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
class PersonRestConfiguration {

    @Bean
    PersonUpdateController personUpdateController(
            final ParentRepository parentRepository,
            final TeacherRepository teacherRepository,
            final PersonRepository personRepository,
            final ApplicationEventPublisher eventPublisher
    ) {
        return new PersonUpdateController(parentRepository, teacherRepository, personRepository, eventPublisher);
    }

    @Bean
    PersonRoleController personRoleController(
            final PersonRepository personRepository,
            final ParentRepository parentRepository,
            final TeacherRepository teacherRepository,
            final ClassroomRepository classroomRepository,
            final @Qualifier("defaultConversionService") ConversionService conversionService
    ) {
        return new PersonRoleController(personRepository, parentRepository, teacherRepository, classroomRepository,
                                        conversionService);
    }

}
