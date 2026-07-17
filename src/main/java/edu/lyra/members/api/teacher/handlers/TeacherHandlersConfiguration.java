package edu.lyra.members.api.teacher.handlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TeacherHandlersConfiguration {

    @Bean
    TeacherRegistrationHandler teacherRegistrationHandler() {
        return new TeacherRegistrationHandler();
    }

    @Bean
    TeacherUpdateAuthorizationEventHandler teacherUpdateAuthorizationEventHandler() {
        return new TeacherUpdateAuthorizationEventHandler();
    }

}
