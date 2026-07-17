package edu.lyra.members.api.parent.handlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ParentHandlersConfiguration {

    @Bean
    ParentRegistrationHandler parentRegistrationHandler() {
        return new ParentRegistrationHandler();
    }

    @Bean
    ParentUpdateAuthorizationEventHandler parentUpdateAuthorizationEventHandler() {
        return new ParentUpdateAuthorizationEventHandler();
    }

}
