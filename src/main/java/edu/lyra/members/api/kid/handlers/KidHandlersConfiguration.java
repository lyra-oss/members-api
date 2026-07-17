package edu.lyra.members.api.kid.handlers;

import edu.lyra.members.api.parent.ParentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KidHandlersConfiguration {

    @Bean
    KidAuthorizationEventHandler kidAuthorizationEventHandler(final ParentRepository parentRepository) {
        return new KidAuthorizationEventHandler(parentRepository);
    }

    @Bean
    KidUpdateAuthorizationEventHandler kidUpdateAuthorizationEventHandler() {
        return new KidUpdateAuthorizationEventHandler();
    }

}
