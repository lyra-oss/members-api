package edu.lyra.members.api.parent.handlers;

import java.util.UUID;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;

@Slf4j
@RequiredArgsConstructor
@RepositoryEventHandler
class ParentRegistrationHandler {

    private final PersonRepository personRepository;

    @HandleBeforeCreate
    public void assignPerson(final Parent parent) {
        final UUID subject = AuthenticatedPrincipal.requireCurrentId();
        log.debug("Resolving person identity {} for parent registration", subject);
        final Person person = this.personRepository.findById(subject).orElseGet(() -> {
            final Person newPerson = parent.getPerson() != null ? parent.getPerson() : Person.builder().build();
            newPerson.setId(subject);
            return newPerson;
        });
        parent.setPerson(person);
    }

}
