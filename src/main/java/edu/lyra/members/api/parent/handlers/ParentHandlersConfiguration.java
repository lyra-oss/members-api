package edu.lyra.members.api.parent.handlers;

import edu.lyra.members.api.person.PersonRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ParentHandlersConfiguration {

    @Bean
    ParentRegistrationHandler parentRegistrationHandler(final PersonRepository personRepository) {
        return new ParentRegistrationHandler(personRepository);
    }

    @Bean
    ParentUpdateAuthorizationEventHandler parentUpdateAuthorizationEventHandler() {
        return new ParentUpdateAuthorizationEventHandler();
    }

}
