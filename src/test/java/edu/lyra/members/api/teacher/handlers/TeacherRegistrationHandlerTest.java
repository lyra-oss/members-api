package edu.lyra.members.api.teacher.handlers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.teacher.Teacher;
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
class TeacherRegistrationHandlerTest {

    @Mock
    private PersonRepository personRepository;

    private TeacherRegistrationHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new TeacherRegistrationHandler(this.personRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reusesTheExistingPersonForTheAuthenticatedSubject() {
        final UUID subject = UUID.randomUUID();
        final Person existingPerson =
                Person.builder().id(subject).name("Marta").surname("Ibáñez").mail("marta.ibanez@example.com").build();
        when(this.personRepository.findById(subject)).thenReturn(Optional.of(existingPerson));
        authenticateAs(subject);
        final Teacher teacher = new Teacher();
        teacher.setName("Someone Else");
        teacher.setSurname("Ignored");
        teacher.setMail("ignored@example.com");
        this.handler.assignPerson(teacher);
        assertSame(existingPerson, teacher.getPerson());
        assertEquals(subject, teacher.getId());
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
        final Teacher teacher = new Teacher();
        teacher.setName("Marta");
        teacher.setSurname("Ibáñez");
        teacher.setMail("marta.ibanez@example.com");
        this.handler.assignPerson(teacher);
        assertEquals(subject, teacher.getPerson().getId());
        assertEquals("Marta", teacher.getPerson().getName());
        assertEquals("Ibáñez", teacher.getPerson().getSurname());
        assertEquals("marta.ibanez@example.com", teacher.getPerson().getMail());
        assertEquals(subject, teacher.getId());
        verify(this.personRepository, never()).save(any());
    }

    @Test
    void buildsAFreshPersonWhenTheSubjectIsUnknownAndNoPersonFieldsWereProvided() {
        final UUID subject = UUID.randomUUID();
        when(this.personRepository.findById(subject)).thenReturn(Optional.empty());
        authenticateAs(subject);
        final Teacher teacher = new Teacher();
        this.handler.assignPerson(teacher);
        assertEquals(subject, teacher.getPerson().getId());
        assertEquals(subject, teacher.getId());
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
        final Teacher teacher = new Teacher();
        assertThrows(AccessDeniedException.class, () -> this.handler.assignPerson(teacher));
        assertNull(teacher.getPerson());
    }

    @Test
    void rejectsNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "credentials"));
        final Teacher teacher = new Teacher();
        assertThrows(AccessDeniedException.class, () -> this.handler.assignPerson(teacher));
    }

}
