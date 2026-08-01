package edu.lyra.members.api.school.rest;

import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.school.SchoolRepository;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SchoolRestConfiguration {

    @Bean
    SchoolMapper schoolMapper() {
        return Mappers.getMapper(SchoolMapper.class);
    }

    @Bean
    SchoolPolicy schoolPolicy() {
        return new SchoolPolicy();
    }

    @Bean
    SchoolAdapter schoolAdapter(
            final SchoolRepository repository,
            final SchoolMapper mapper,
            final SchoolPolicy policy,
            final ApiBasePath apiBasePath
    ) {
        return new SchoolAdapter(repository, mapper, policy, apiBasePath);
    }

}
