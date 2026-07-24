package edu.lyra.members.api.parent.handlers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentRegistrationHandlerTest {

    @Mock
    private PersonRepository personRepository;

    private ParentRegistrationHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new ParentRegistrationHandler(this.personRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reusesTheExistingPersonForTheAuthenticatedSubject() {
        final UUID subject = UUID.randomUUID();
        //@formatter:off
        final Person existingPerson = Person.builder()
                                            .id(subject)
                                            .name("Esteban")
                                            .surname("Cristóbal")
                                            .mail("esteban.cristobal@example.com")
                                            .build();
        //@formatter:on
        when(this.personRepository.findById(subject)).thenReturn(Optional.of(existingPerson));
        authenticateAs(subject);
        final Parent parent = new Parent();
        parent.setName("Someone Else");
        parent.setSurname("Ignored");
        parent.setMail("ignored@example.com");
        this.handler.assignPerson(parent);
        assertSame(existingPerson, parent.getPerson());
        assertEquals(subject, parent.getId());
    }

    private static void authenticateAs(final UUID subject) {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject(subject.toString())
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @Test
    void buildsANewPersonFromThePayloadWhenTheSubjectIsUnknown() {
        final UUID subject = UUID.randomUUID();
        when(this.personRepository.findById(subject)).thenReturn(Optional.empty());
        authenticateAs(subject);
        final Parent parent = new Parent();
        parent.setName("Esteban");
        parent.setSurname("Cristóbal");
        parent.setMail("esteban.cristobal@example.com");
        this.handler.assignPerson(parent);
        assertEquals(subject, parent.getPerson().getId());
        assertEquals("Esteban", parent.getPerson().getName());
        assertEquals("Cristóbal", parent.getPerson().getSurname());
        assertEquals("esteban.cristobal@example.com", parent.getPerson().getMail());
        assertEquals(subject, parent.getId());
        verify(this.personRepository, never()).save(any());
    }

    @Test
    void buildsAFreshPersonWhenTheSubjectIsUnknownAndNoPersonFieldsWereProvided() {
        final UUID subject = UUID.randomUUID();
        when(this.personRepository.findById(subject)).thenReturn(Optional.empty());
        authenticateAs(subject);
        final Parent parent = new Parent();
        this.handler.assignPerson(parent);
        assertEquals(subject, parent.getPerson().getId());
        assertEquals(subject, parent.getId());
    }

    @Test
    void rejectsMissingSubjectClaim() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .claim("preferred_username", "john")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Parent parent = new Parent();
        assertThrows(AccessDeniedException.class, () -> this.handler.assignPerson(parent));
        assertNull(parent.getPerson());
    }

    @Test
    void rejectsNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "credentials"));
        final Parent parent = new Parent();
        assertThrows(AccessDeniedException.class, () -> this.handler.assignPerson(parent));
    }

}
