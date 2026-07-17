package edu.lyra.members.api.contactinfo.rest;

import edu.lyra.members.api.parent.ParentsRepository;
import edu.lyra.members.api.teacher.TeachersRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ContactInfoRestConfiguration {

    @Bean
    ContactInfoUpdateController contactInfoUpdateController(
            final ParentsRepository parentsRepository,
            final TeachersRepository teachersRepository,
            final ApplicationEventPublisher eventPublisher
    ) {
        return new ContactInfoUpdateController(parentsRepository, teachersRepository, eventPublisher);
    }

}
