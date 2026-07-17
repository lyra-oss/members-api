package edu.lyra.members.api.person.rest;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static java.util.Map.of;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonRoleControllerTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private ConversionService conversionService;

    private PersonRoleController controller;

    @BeforeEach
    void setUp() {
        this.controller = new PersonRoleController(this.personRepository, this.parentRepository, this.teacherRepository,
                                                   this.classroomRepository, this.conversionService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantParentRoleRejectsNonAdmin() {
        authenticateAsNonAdmin();
        final UUID id = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> this.controller.grantParentRole(id));
        verify(this.personRepository, never()).findById(any());
    }

    private static void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "n/a", List.of()));
    }

    @Test
    void grantParentRoleReturnsNotFoundWhenPersonMissing() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.grantParentRole(id).getStatusCode());
    }

    private static void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }

    @Test
    void grantParentRoleIsIdempotentWhenAlreadyAParent() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        when(this.parentRepository.existsById(id)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, this.controller.grantParentRole(id).getStatusCode());
        verify(this.parentRepository, never()).save(any());
    }

    private static Person aPerson(final UUID id) {
        //@formatter:off
        return Person.builder().id(id)
                     .name("Esteban")
                     .surname("Cristóbal")
                     .mail("esteban.cristobal@example.com")
                     .build();
        //@formatter:on
    }

    @Test
    void grantParentRoleSavesANewParentForAnUnclaimedPerson() {
        authenticateAsAdmin();
        final UUID   id     = UUID.randomUUID();
        final Person person = aPerson(id);
        when(this.personRepository.findById(id)).thenReturn(Optional.of(person));
        when(this.parentRepository.existsById(id)).thenReturn(false);
        assertEquals(HttpStatus.NO_CONTENT, this.controller.grantParentRole(id).getStatusCode());
        final ArgumentCaptor<Parent> captor = ArgumentCaptor.forClass(Parent.class);
        verify(this.parentRepository).save(captor.capture());
        assertEquals(person, captor.getValue().getPerson());
    }

    @Test
    void grantTeacherRoleRejectsNonAdmin() {
        authenticateAsNonAdmin();
        final UUID id = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> this.controller.grantTeacherRole(id, of()));
        verify(this.personRepository, never()).findById(any());
    }

    @Test
    void grantTeacherRoleReturnsNotFoundWhenPersonMissing() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.grantTeacherRole(id, of()).getStatusCode());
    }

    @Test
    void grantTeacherRoleIsIdempotentWhenAlreadyATeacher() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        when(this.teacherRepository.existsById(id)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, this.controller.grantTeacherRole(id, of()).getStatusCode());
        verify(this.teacherRepository, never()).save(any());
    }

    @Test
    void grantTeacherRoleRejectsMissingSchool() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        when(this.teacherRepository.existsById(id)).thenReturn(false);
        assertEquals(HttpStatus.BAD_REQUEST, this.controller.grantTeacherRole(id, of()).getStatusCode());
        verify(this.teacherRepository, never()).save(any());
    }

    @Test
    void grantTeacherRoleRejectsAnUnresolvableSchool() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        when(this.teacherRepository.existsById(id)).thenReturn(false);
        when(this.conversionService.convert(any(URI.class), eq(School.class))).thenReturn(null);
        //@formatter:off
        final var response = this.controller.grantTeacherRole(id, of("school", "/v0/schools/" + UUID.randomUUID()));
        //@formatter:on
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.teacherRepository, never()).save(any());
    }

    @Test
    void grantTeacherRoleSavesANewTeacherWithTheResolvedSchool() {
        authenticateAsAdmin();
        final UUID   id     = UUID.randomUUID();
        final Person person = aPerson(id);
        final School school = mock(School.class);
        when(this.personRepository.findById(id)).thenReturn(Optional.of(person));
        when(this.teacherRepository.existsById(id)).thenReturn(false);
        when(this.conversionService.convert(any(URI.class), eq(School.class))).thenReturn(school);
        //@formatter:off
        final var response = this.controller.grantTeacherRole(id, of("school", "/v0/schools/" + UUID.randomUUID()));
        //@formatter:on
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        final ArgumentCaptor<Teacher> captor = ArgumentCaptor.forClass(Teacher.class);
        verify(this.teacherRepository).save(captor.capture());
        assertEquals(school, captor.getValue().getSchool());
        assertEquals(person, captor.getValue().getPerson());
    }

    @Test
    void revokeParentRoleRejectsNonAdmin() {
        authenticateAsNonAdmin();
        final UUID id = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> this.controller.revokeParentRole(id));
        verify(this.parentRepository, never()).findById(any());
    }

    @Test
    void revokeParentRoleReturnsNotFoundWhenNotAParent() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.parentRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.revokeParentRole(id).getStatusCode());
    }

    @Test
    void revokeParentRoleRejectsWhenParentStillHasKids() {
        authenticateAsAdmin();
        final UUID   id     = UUID.randomUUID();
        final Parent parent = mock(Parent.class);
        when(parent.getKids()).thenReturn(Set.of(mock(Kid.class)));
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        assertEquals(HttpStatus.CONFLICT, this.controller.revokeParentRole(id).getStatusCode());
        verify(this.parentRepository, never()).delete(any());
    }

    @Test
    void revokeParentRoleDeletesTheParentWhenChildless() {
        authenticateAsAdmin();
        final UUID   id     = UUID.randomUUID();
        final Parent parent = mock(Parent.class);
        when(parent.getKids()).thenReturn(Set.of());
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        assertEquals(HttpStatus.NO_CONTENT, this.controller.revokeParentRole(id).getStatusCode());
        verify(this.parentRepository).delete(parent);
    }

    @Test
    void revokeTeacherRoleRejectsNonAdmin() {
        authenticateAsNonAdmin();
        final UUID id = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> this.controller.revokeTeacherRole(id));
        verify(this.teacherRepository, never()).findById(any());
    }

    @Test
    void revokeTeacherRoleReturnsNotFoundWhenNotATeacher() {
        authenticateAsAdmin();
        final UUID id = UUID.randomUUID();
        when(this.teacherRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.revokeTeacherRole(id).getStatusCode());
    }

    @Test
    void revokeTeacherRoleRejectsWhenReferencedByAClassroom() {
        authenticateAsAdmin();
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = mock(Teacher.class);
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        when(this.classroomRepository.existsByTutorIdOrTeachersId(id)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, this.controller.revokeTeacherRole(id).getStatusCode());
        verify(this.teacherRepository, never()).delete(any());
    }

    @Test
    void revokeTeacherRoleDeletesTheTeacherWhenUnreferenced() {
        authenticateAsAdmin();
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = mock(Teacher.class);
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        when(this.classroomRepository.existsByTutorIdOrTeachersId(id)).thenReturn(false);
        assertEquals(HttpStatus.NO_CONTENT, this.controller.revokeTeacherRole(id).getStatusCode());
        verify(this.teacherRepository).delete(teacher);
    }

}
