package edu.lyra.members.api.teacher.rest;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.exceptions.TeacherAssignedToClassroomException;
import edu.lyra.members.api.person.PersonRole;
import edu.lyra.members.api.teacher.Teacher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;

import static org.instancio.Instancio.of;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TeacherPolicyTest {

    @Mock
    private ClassroomRepository classroomRepository;

    private TeacherPolicy policy;

    @BeforeEach
    void setUp() {
        lenient().when(this.classroomRepository.existsByTutorIdOrTeachersId(any())).thenReturn(false);
        this.policy = new TeacherPolicy(this.classroomRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToUpdateAnyTeacher() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aTeacherWithId(randomUUID())));
    }

    private static Teacher aTeacherWithId(final UUID id) {
        return of(Teacher.class).set(field(PersonRole.class, "id"), id).create();
    }

    @Test
    void allowsTeacherToUpdateOwnAccount() {
        final UUID id = randomUUID();
        authenticateAs(id, "teacher");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aTeacherWithId(id)));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void rejectsTeacherUpdatingAnotherTeachersAccount() {
        authenticateAs(randomUUID(), "teacher");
        final Teacher teacher = aTeacherWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(teacher));
    }

    @Test
    void rejectsParentUpdatingATeacher() {
        authenticateAs(randomUUID(), "parent");
        final Teacher teacher = aTeacherWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(teacher));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Teacher teacher = aTeacherWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(teacher));
    }

    @Test
    void allowsAdminToDeleteAnUnreferencedTeacher() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aTeacherWithId(randomUUID())));
    }

    @Test
    void rejectsAdminDeletingATeacherStillReferencedByAClassroom() {
        authenticateAs(randomUUID(), "admin");
        final UUID id = randomUUID();
        lenient().when(this.classroomRepository.existsByTutorIdOrTeachersId(id)).thenReturn(true);
        final Teacher teacher = aTeacherWithId(id);
        assertThrows(TeacherAssignedToClassroomException.class, () -> this.policy.authorizeDelete(teacher));
    }

    @Test
    void allowsATeacherToDeleteTheirOwnUnreferencedAccount() {
        final UUID id = randomUUID();
        authenticateAs(id, "teacher");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aTeacherWithId(id)));
    }

    @Test
    void rejectsATeacherDeletingTheirOwnAccountWhileStillReferencedByAClassroom() {
        final UUID id = randomUUID();
        authenticateAs(id, "teacher");
        lenient().when(this.classroomRepository.existsByTutorIdOrTeachersId(id)).thenReturn(true);
        final Teacher teacher = aTeacherWithId(id);
        assertThrows(TeacherAssignedToClassroomException.class, () -> this.policy.authorizeDelete(teacher));
    }

    @Test
    void rejectsATeacherDeletingAnotherTeachersAccount() {
        authenticateAs(randomUUID(), "teacher");
        final Teacher teacher = aTeacherWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(teacher));
    }

    @Test
    void rejectsParentDeletingATeacher() {
        authenticateAs(randomUUID(), "parent");
        final Teacher teacher = aTeacherWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(teacher));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final Teacher teacher = aTeacherWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(teacher));
    }

}
