package edu.lyra.members.api.school.handlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SchoolHandlersConfiguration {

    @Bean
    SchoolUpdateAuthorizationEventHandler schoolUpdateAuthorizationEventHandler() {
        return new SchoolUpdateAuthorizationEventHandler();
    }

    @Bean
    SchoolDeleteEventHandler schoolDeleteEventHandler() {
        return new SchoolDeleteEventHandler();
    }

}
