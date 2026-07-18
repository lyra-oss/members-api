package edu.lyra.members.api.teacher.handlers;

import java.util.UUID;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.teacher.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;

@Slf4j
@RequiredArgsConstructor
@RepositoryEventHandler
class TeacherRegistrationHandler {

    private final PersonRepository personRepository;

    @HandleBeforeCreate
    public void assignPerson(final Teacher teacher) {
        final UUID subject = AuthenticatedPrincipal.requireCurrentId();
        log.debug("Resolving person identity {} for teacher registration", subject);
        final Person person = this.personRepository.findById(subject).orElseGet(() -> {
            final Person newPerson = teacher.getPerson() != null ? teacher.getPerson() : Person.builder().build();
            newPerson.setId(subject);
            return newPerson;
        });
        teacher.setPerson(person);
    }

}
