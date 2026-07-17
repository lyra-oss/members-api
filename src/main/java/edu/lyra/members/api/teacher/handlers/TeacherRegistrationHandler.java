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

/**
 * Resolves the {@link Person} behind a self-registering teacher: reuses the person already known for the authenticated
 * subject (e.g. someone already registered as a parent becoming a teacher too, in which case the identity fields in
 * this payload are ignored), or builds a new, not-yet-persisted one from this payload's contact info — either way,
 * persistence happens once, atomically with the {@link Teacher} row, via the {@code cascade} on
 * {@link Teacher#getPerson()}.
 */
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
