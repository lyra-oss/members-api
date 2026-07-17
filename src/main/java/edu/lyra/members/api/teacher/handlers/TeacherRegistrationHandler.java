package edu.lyra.members.api.teacher.handlers;

import java.util.UUID;

import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.teacher.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Optional.ofNullable;

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
        final UUID subject = currentSubject();
        log.debug("Resolving person identity {} for teacher registration", subject);
        final Person person = this.personRepository.findById(subject).orElseGet(() -> {
            final Person newPerson = teacher.getPerson() != null ? teacher.getPerson() : Person.builder().build();
            newPerson.setId(subject);
            return newPerson;
        });
        teacher.setPerson(person);
    }

    private static UUID currentSubject() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException("JWT authentication required");
        }
        //@formatter:off
        return ofNullable(jwtAuth.getToken().getSubject()).map(UUID::fromString)
                .orElseThrow(() -> new AccessDeniedException("Missing \"sub\" claim in JWT token"));
        //@formatter:on
    }

}
