package edu.lyra.members.api.parent.rest;

import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.PersonRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ParentRestConfiguration {

    @Bean
    ParentPolicy parentPolicy() {
        return new ParentPolicy();
    }

    @Bean
    ParentAdapter parentAdapter(
            final ParentRepository parentRepository,
            final KidRepository kidRepository,
            final PersonRepository personRepository,
            final ParentMapper mapper,
            final ParentPolicy policy
    ) {
        return new ParentAdapter(parentRepository, kidRepository, personRepository, mapper, policy);
    }

}
