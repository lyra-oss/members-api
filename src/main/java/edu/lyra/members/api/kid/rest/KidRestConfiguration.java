package edu.lyra.members.api.kid.rest;

import edu.lyra.members.api.kid.KidsRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KidRestConfiguration {

    @Bean
    KidsCollectionController kidsCollectionController(final KidsRepository kidsRepository) {
        return new KidsCollectionController(kidsRepository);
    }

}
