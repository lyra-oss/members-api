package edu.lyra.members.api.kid.handlers;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.person.PersonRole;
import edu.lyra.members.api.teacher.Teacher;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Arrays.stream;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KidDeleteAuthorizationEventHandlerTest {

    private final KidDeleteAuthorizationEventHandler handler = new KidDeleteAuthorizationEventHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToDeleteAnyKid() {
        authenticateAs(UUID.randomUUID(), "admin");
        final Kid kid = aKid(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertDoesNotThrow(() -> this.handler.authorizeKidDelete(kid));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Kid aKid(final Parent parent, final Classroom classroom) {
        return Instancio.of(Kid.class).set(field(Kid.class, "parent"), parent)
                        .set(field(Kid.class, "classroom"), classroom).create();
    }

    private static Parent aParent() {
        return aParentWithId(UUID.randomUUID());
    }

    private static Classroom aClassroomWithTutor(final UUID tutorId) {
        final Teacher tutor = Instancio.of(Teacher.class).set(field(PersonRole.class, "id"), tutorId).create();
        return Instancio.of(Classroom.class).set(field(Classroom.class, "tutor"), tutor)
                        .ignore(field(Classroom.class, "teachers")).ignore(field(Classroom.class, "kids")).create();
    }

    private static Parent aParentWithId(final UUID id) {
        return Instancio.of(Parent.class).set(field(PersonRole.class, "id"), id).ignore(field(Parent.class, "kids"))
                        .create();
    }

    @Test
    void allowsParentToDeleteTheirOwnKid() {
        final UUID   parentId = UUID.randomUUID();
        final Parent parent   = aParentWithId(parentId);
        authenticateAs(parentId, "parent");
        final Kid kid = aKid(parent, aClassroomWithTutor(UUID.randomUUID()));
        assertDoesNotThrow(() -> this.handler.authorizeKidDelete(kid));
    }

    @Test
    void rejectsParentDeletingAnotherParentsKid() {
        authenticateAs(UUID.randomUUID(), "parent");
        final Kid kid = aKid(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidDelete(kid));
    }

    @Test
    void allowsTutorToDeleteAKidInTheirClassroom() {
        final UUID tutorId = UUID.randomUUID();
        authenticateAs(tutorId, "teacher");
        final Kid kid = aKid(aParent(), aClassroomWithTutor(tutorId));
        assertDoesNotThrow(() -> this.handler.authorizeKidDelete(kid));
    }

    @Test
    void rejectsNonTutorTeacherOfTheClassroomDeletingAKid() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Kid kid = aKid(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidDelete(kid));
    }

    @Test
    void rejectsDeletingAKidWithNoClassroomAssignedByATeacher() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Kid kid = aKid(aParent(), null);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidDelete(kid));
    }

    @Test
    void rejectsDeletingAKidWithNoParentAssignedByAParent() {
        authenticateAs(UUID.randomUUID(), "parent");
        final Kid kid = aKid(null, aClassroomWithTutor(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidDelete(kid));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final Kid kid = aKid(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidDelete(kid));
    }

}
