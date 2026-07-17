package edu.lyra.members.api.teacher.handlers;

import edu.lyra.members.api.person.PersonRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TeacherHandlersConfiguration {

    @Bean
    TeacherRegistrationHandler teacherRegistrationHandler(final PersonRepository personRepository) {
        return new TeacherRegistrationHandler(personRepository);
    }

    @Bean
    TeacherUpdateAuthorizationEventHandler teacherUpdateAuthorizationEventHandler() {
        return new TeacherUpdateAuthorizationEventHandler();
    }

}
