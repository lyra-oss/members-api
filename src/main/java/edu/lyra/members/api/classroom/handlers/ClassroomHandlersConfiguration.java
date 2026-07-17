package edu.lyra.members.api.classroom.handlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClassroomHandlersConfiguration {

    @Bean
    ClassroomTeacherAssignmentEventHandler classroomTeacherAssignmentEventHandler() {
        return new ClassroomTeacherAssignmentEventHandler();
    }

    @Bean
    ClassroomUpdateAuthorizationEventHandler classroomUpdateAuthorizationEventHandler() {
        return new ClassroomUpdateAuthorizationEventHandler();
    }

}
