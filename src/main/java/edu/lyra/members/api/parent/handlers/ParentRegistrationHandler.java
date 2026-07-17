package edu.lyra.members.api.parent.handlers;

import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
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
 * Resolves the {@link Person} behind a self-registering parent: reuses the person already known for the authenticated
 * subject (e.g. someone already registered as a teacher becoming a parent too, in which case the identity fields in
 * this payload are ignored), or builds a new, not-yet-persisted one from this payload's contact info — either way,
 * persistence happens once, atomically with the {@link Parent} row, via the {@code cascade} on
 * {@link Parent#getPerson()}.
 */
@Slf4j
@RequiredArgsConstructor
@RepositoryEventHandler
class ParentRegistrationHandler {

    private final PersonRepository personRepository;

    @HandleBeforeCreate
    public void assignPerson(final Parent parent) {
        final UUID subject = currentSubject();
        log.debug("Resolving person identity {} for parent registration", subject);
        final Person person = this.personRepository.findById(subject).orElseGet(() -> {
            final Person newPerson = parent.getPerson() != null ? parent.getPerson() : Person.builder().build();
            newPerson.setId(subject);
            return newPerson;
        });
        parent.setPerson(person);
    }

    private static UUID currentSubject() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException("JWT authentication required");
        }
        return ofNullable(jwtAuth.getToken().getSubject()).map(UUID::fromString).orElseThrow(
                () -> new AccessDeniedException("Missing \"sub\" claim in JWT token"));
    }

}
