package edu.lyra.members.api.handlers;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.Teacher;
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

class KidUpdateAuthorizationEventHandlerTest {

    private final KidUpdateAuthorizationEventHandler handler = new KidUpdateAuthorizationEventHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToUpdateAnyKid() {
        authenticateAs(UUID.randomUUID(), "admin");
        final Kid kid = aKidUnchanged(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertDoesNotThrow(() -> this.handler.authorizeKidUpdate(kid));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** A kid whose parent/classroom on the incoming object match what {@code @PostLoad} would have captured. */
    private static Kid aKidUnchanged(final Parent parent, final Classroom classroom) {
        //@formatter:off
        return Instancio.of(Kid.class)
                        .set(field(Kid.class, "parent"), parent)
                        .set(field(Kid.class, "previousParentId"), parent.getId())
                        .set(field(Kid.class, "classroom"), classroom)
                        .set(field(Kid.class, "previousClassroomId"), classroom == null ? null : classroom.getId())
                        .create();
        //@formatter:on
    }

    private static Parent aParent() {
        return aParentWithId(UUID.randomUUID());
    }

    private static Classroom aClassroomWithTutor(final UUID tutorId) {
        final Teacher tutor = Instancio.of(Teacher.class).set(field(Teacher.class, "id"), tutorId).create();
        return Instancio.of(Classroom.class).set(field(Classroom.class, "tutor"), tutor)
                        .ignore(field(Classroom.class, "teachers")).ignore(field(Classroom.class, "kids")).create();
    }

    private static Parent aParentWithId(final UUID id) {
        return Instancio.of(Parent.class).set(field(Parent.class, "id"), id).ignore(field(Parent.class, "kids"))
                        .create();
    }

    @Test
    void allowsParentToUpdateOwnKidsFields() {
        final UUID   parentId = UUID.randomUUID();
        final Parent parent   = aParentWithId(parentId);
        authenticateAs(parentId, "parent");
        final Kid kid = aKidUnchanged(parent, aClassroomWithTutor(UUID.randomUUID()));
        assertDoesNotThrow(() -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void rejectsParentUpdatingAnotherParentsKid() {
        authenticateAs(UUID.randomUUID(), "parent");
        final Kid kid = aKidUnchanged(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void rejectsParentRebindingTheirOwnKidToADifferentParent() {
        final UUID parentId = UUID.randomUUID();
        authenticateAs(parentId, "parent");
        //@formatter:off
        final Kid kid = Instancio.of(Kid.class)
                                 .set(field(Kid.class, "parent"), aParent())
                                 .set(field(Kid.class, "previousParentId"), parentId)
                                 .set(field(Kid.class, "classroom"), (Classroom) null)
                                 .set(field(Kid.class, "previousClassroomId"), (UUID) null)
                                 .create();
        //@formatter:on
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void allowsTutorToUpdateFieldsOfAKidInTheirClassroom() {
        final UUID tutorId = UUID.randomUUID();
        authenticateAs(tutorId, "teacher");
        final Classroom classroom = aClassroomWithTutor(tutorId);
        final Kid       kid       = aKidUnchanged(aParent(), classroom);
        assertDoesNotThrow(() -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void rejectsNonTutorTeacherOfTheClassroomUpdatingAKidsFields() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Kid kid = aKidUnchanged(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void rejectsUpdatingAKidWithNoClassroomAssignedByATeacher() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Kid kid = aKidUnchanged(aParent(), null);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void allowsTargetClassroomsTutorToEnrollAKid() {
        final UUID tutorId = UUID.randomUUID();
        authenticateAs(tutorId, "teacher");
        final Parent    parent       = aParent();
        final Classroom newClassroom = aClassroomWithTutor(tutorId);
        //@formatter:off
        final Kid enrolled = Instancio.of(Kid.class)
                                      .set(field(Kid.class, "parent"), parent)
                                      .set(field(Kid.class, "previousParentId"), parent.getId())
                                      .set(field(Kid.class, "classroom"), newClassroom)
                                      .set(field(Kid.class, "previousClassroomId"), (UUID) null)
                                      .create();
        //@formatter:on
        assertDoesNotThrow(() -> this.handler.authorizeKidUpdate(enrolled));
    }

    @Test
    void rejectsEnrollingAKidIntoAClassroomTheActingTeacherDoesNotTutor() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Parent    parent       = aParent();
        final Classroom newClassroom = aClassroomWithTutor(UUID.randomUUID());
        //@formatter:off
        final Kid kid = Instancio.of(Kid.class)
                                 .set(field(Kid.class, "parent"), parent)
                                 .set(field(Kid.class, "previousParentId"), parent.getId())
                                 .set(field(Kid.class, "classroom"), newClassroom)
                                 .set(field(Kid.class, "previousClassroomId"), (UUID) null)
                                 .create();
        //@formatter:on
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void rejectsParentEnrollingTheirOwnKidIntoAClassroom() {
        final UUID parentId = UUID.randomUUID();
        authenticateAs(parentId, "parent");
        final Parent    parent       = aParentWithId(parentId);
        final Classroom newClassroom = aClassroomWithTutor(UUID.randomUUID());
        //@formatter:off
        final Kid kid = Instancio.of(Kid.class)
                                 .set(field(Kid.class, "parent"), parent)
                                 .set(field(Kid.class, "previousParentId"), parentId)
                                 .set(field(Kid.class, "classroom"), newClassroom)
                                 .set(field(Kid.class, "previousClassroomId"), (UUID) null)
                                 .create();
        //@formatter:on
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidUpdate(kid));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Kid kid = aKidUnchanged(aParent(), aClassroomWithTutor(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidUpdate(kid));
    }

}
