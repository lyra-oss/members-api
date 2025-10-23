package com.sagittec.lyra.members.api.controllers;

import com.sagittec.lyra.members.api.repositories.ParentsRepository;
import com.sagittec.lyra.members.api.repositories.SchoolsRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SpringWebConfiguration {

    @Bean
    ParentsController parentsController(final ParentsRepository parentsRepository) {
        return new ParentsController(parentsRepository);
    }

    @Bean
    SchoolsController schoolsController(final SchoolsRepository schoolsRepository) {
        return new SchoolsController(schoolsRepository);
    }

}
