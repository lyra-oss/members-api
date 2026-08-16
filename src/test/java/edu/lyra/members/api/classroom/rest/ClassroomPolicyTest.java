package edu.lyra.members.api.classroom.rest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.exceptions.ClassroomHasKidsException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.person.PersonRole;
import edu.lyra.members.api.teacher.Teacher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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

class ClassroomPolicyTest {

    private final ClassroomPolicy policy = new ClassroomPolicy();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToUpdateAnyClassroom() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aClassroom(aTeacherWithId(randomUUID()), Set.of())));
    }

    private static Classroom aClassroom(final Teacher tutor, final Set<Kid> kids) {
        //@formatter:off
        return of(Classroom.class).set(field(Classroom.class, "tutor"), tutor)
                                  .ignore(field(Classroom.class, "teachers"))
                                  .set(field(Classroom.class, "kids"), kids)
                                  .create();
        //@formatter:on
    }

    private static Teacher aTeacherWithId(final UUID id) {
        return of(Teacher.class).set(field(PersonRole.class, "id"), id).create();
    }

    @Test
    void allowsCurrentTutorToUpdateTheClassroom() {
        final UUID tutorId = randomUUID();
        authenticateAs(tutorId, "teacher");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aClassroom(aTeacherWithId(tutorId), Set.of())));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void rejectsNonTutorTeacherUpdatingTheClassroom() {
        authenticateAs(randomUUID(), "teacher");
        final Classroom classroom = aClassroom(aTeacherWithId(randomUUID()), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(classroom));
    }

    @Test
    void rejectsUpdatingAClassroomWithNoTutorByATeacher() {
        authenticateAs(randomUUID(), "teacher");
        final Classroom classroom = aClassroom(null, Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(classroom));
    }

    @Test
    void rejectsParentUpdatingAClassroom() {
        authenticateAs(randomUUID(), "parent");
        final Classroom classroom = aClassroom(aTeacherWithId(randomUUID()), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(classroom));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Classroom classroom = aClassroom(aTeacherWithId(randomUUID()), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(classroom));
    }

    @Test
    void allowsAdminToDeleteAClassroomWithoutKids() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aClassroom(aTeacherWithId(randomUUID()), Set.of())));
    }

    @Test
    void allowsCurrentTutorToDeleteTheirClassroomWithoutKids() {
        final UUID tutorId = randomUUID();
        authenticateAs(tutorId, "teacher");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aClassroom(aTeacherWithId(tutorId), Set.of())));
    }

    @Test
    void rejectsAdminDeletingAClassroomThatStillHasKids() {
        authenticateAs(randomUUID(), "admin");
        final Classroom classroom = aClassroom(aTeacherWithId(randomUUID()), Set.of(of(Kid.class).create()));
        assertThrows(ClassroomHasKidsException.class, () -> this.policy.authorizeDelete(classroom));
    }

    @Test
    void rejectsCurrentTutorDeletingTheirClassroomWhileItStillHasKids() {
        final UUID tutorId = randomUUID();
        authenticateAs(tutorId, "teacher");
        final Classroom classroom = aClassroom(aTeacherWithId(tutorId), Set.of(of(Kid.class).create()));
        assertThrows(ClassroomHasKidsException.class, () -> this.policy.authorizeDelete(classroom));
    }

    @Test
    void rejectsNonTutorTeacherDeletingTheClassroom() {
        authenticateAs(randomUUID(), "teacher");
        final Classroom classroom = aClassroom(aTeacherWithId(randomUUID()), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(classroom));
    }

    @Test
    void rejectsDeletingAClassroomWithNoTutorByATeacher() {
        authenticateAs(randomUUID(), "teacher");
        final Classroom classroom = aClassroom(null, Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(classroom));
    }

    @Test
    void rejectsParentDeletingAClassroom() {
        authenticateAs(randomUUID(), "parent");
        final Classroom classroom = aClassroom(aTeacherWithId(randomUUID()), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(classroom));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final Classroom classroom = aClassroom(aTeacherWithId(randomUUID()), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(classroom));
    }

}
