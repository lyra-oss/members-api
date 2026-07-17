package edu.lyra.members.api.kid.rest;

import edu.lyra.members.api.kid.KidRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KidRestConfiguration {

    @Bean
    KidsCollectionController kidsCollectionController(final KidRepository kidRepository) {
        return new KidsCollectionController(kidRepository);
    }

}
