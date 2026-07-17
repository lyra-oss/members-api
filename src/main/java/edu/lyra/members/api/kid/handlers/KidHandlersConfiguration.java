package edu.lyra.members.api.kid.handlers;

import edu.lyra.members.api.parent.ParentsRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KidHandlersConfiguration {

    @Bean
    KidAuthorizationEventHandler kidAuthorizationEventHandler(final ParentsRepository parentsRepository) {
        return new KidAuthorizationEventHandler(parentsRepository);
    }

    @Bean
    KidUpdateAuthorizationEventHandler kidUpdateAuthorizationEventHandler() {
        return new KidUpdateAuthorizationEventHandler();
    }

}
