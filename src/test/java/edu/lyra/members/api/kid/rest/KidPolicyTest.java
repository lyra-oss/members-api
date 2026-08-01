package edu.lyra.members.api.kid.rest;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
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

class KidPolicyTest {

    private final KidPolicy policy = new KidPolicy();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Kid aKid(final Parent parent, final Classroom classroom) {
        //@formatter:off
        return of(Kid.class).set(field(Kid.class, "parent"), parent)
                            .set(field(Kid.class, "classroom"), classroom)
                            .create();
        //@formatter:on
    }

    private static Parent aParentWithId(final UUID id) {
        return of(Parent.class).set(field(PersonRole.class, "id"), id).ignore(field(Parent.class, "kids")).create();
    }

    private static Classroom aClassroomWithTutor(final UUID tutorId) {
        final Teacher tutor = of(Teacher.class).set(field(PersonRole.class, "id"), tutorId).create();
        //@formatter:off
        return of(Classroom.class).set(field(Classroom.class, "tutor"), tutor)
                                  .ignore(field(Classroom.class, "teachers"))
                                  .ignore(field(Classroom.class, "kids"))
                                  .create();
        //@formatter:on
    }

    // --- authorizeUpdate: plain field update (parent and classroom unchanged) ---

    @Test
    void allowsAdminToUpdateAnyKid() {
        authenticateAs(randomUUID(), "admin");
        final Parent    parent    = aParentWithId(randomUUID());
        final Classroom classroom = aClassroomWithTutor(randomUUID());
        final Kid       kid       = aKid(parent, classroom);
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(kid, parent, classroom));
    }

    @Test
    void allowsParentToUpdateOwnKidsFields() {
        final UUID       parentId  = randomUUID();
        final Parent     parent    = aParentWithId(parentId);
        final Classroom  classroom = aClassroomWithTutor(randomUUID());
        final Kid        kid       = aKid(parent, classroom);
        authenticateAs(parentId, "parent");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(kid, parent, classroom));
    }

    @Test
    void rejectsParentUpdatingAnotherParentsKid() {
        authenticateAs(randomUUID(), "parent");
        final Parent    parent    = aParentWithId(randomUUID());
        final Classroom classroom = aClassroomWithTutor(randomUUID());
        final Kid       kid       = aKid(parent, classroom);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(kid, parent, classroom));
    }

    @Test
    void allowsTutorToUpdateFieldsOfAKidInTheirClassroom() {
        final UUID      tutorId   = randomUUID();
        final Classroom classroom = aClassroomWithTutor(tutorId);
        final Parent    parent    = aParentWithId(randomUUID());
        final Kid       kid       = aKid(parent, classroom);
        authenticateAs(tutorId, "teacher");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(kid, parent, classroom));
    }

    @Test
    void rejectsNonTutorTeacherOfTheClassroomUpdatingAKidsFields() {
        authenticateAs(randomUUID(), "teacher");
        final Parent    parent    = aParentWithId(randomUUID());
        final Classroom classroom = aClassroomWithTutor(randomUUID());
        final Kid       kid       = aKid(parent, classroom);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(kid, parent, classroom));
    }

    @Test
    void rejectsUpdatingAKidWithNoClassroomAssignedByATeacher() {
        authenticateAs(randomUUID(), "teacher");
        final Parent parent = aParentWithId(randomUUID());
        final Kid    kid    = aKid(parent, null);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(kid, parent, null));
    }

    @Test
    void allowsTutorToUpdateFieldsOfAKidWithNoParentAssigned() {
        final UUID      tutorId   = randomUUID();
        final Classroom classroom = aClassroomWithTutor(tutorId);
        final Kid       kid       = aKid(null, classroom);
        authenticateAs(tutorId, "teacher");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(kid, null, classroom));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Parent    parent    = aParentWithId(randomUUID());
        final Classroom classroom = aClassroomWithTutor(randomUUID());
        final Kid       kid       = aKid(parent, classroom);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(kid, parent, classroom));
    }

    // --- authorizeUpdate: rebinding to a different parent ---

    @Test
    void rejectsParentRebindingTheirOwnKidToADifferentParent() {
        final UUID   parentId  = randomUUID();
        final Parent newParent = aParentWithId(randomUUID());
        final Kid    kid       = aKid(aParentWithId(parentId), null);
        authenticateAs(parentId, "parent");
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(kid, newParent, null));
    }

    @Test
    void allowsAdminToRebindAKidToADifferentParent() {
        authenticateAs(randomUUID(), "admin");
        final Parent newParent = aParentWithId(randomUUID());
        final Kid    kid       = aKid(aParentWithId(randomUUID()), null);
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(kid, newParent, null));
    }

    // --- authorizeUpdate: enrolling into a different classroom ---

    @Test
    void allowsTargetClassroomsTutorToEnrollAKid() {
        final UUID      tutorId      = randomUUID();
        final Parent    parent       = aParentWithId(randomUUID());
        final Classroom newClassroom = aClassroomWithTutor(tutorId);
        final Kid        kid         = aKid(parent, null);
        authenticateAs(tutorId, "teacher");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(kid, parent, newClassroom));
    }

    @Test
    void rejectsEnrollingAKidIntoAClassroomTheActingTeacherDoesNotTutor() {
        authenticateAs(randomUUID(), "teacher");
        final Parent    parent       = aParentWithId(randomUUID());
        final Classroom newClassroom = aClassroomWithTutor(randomUUID());
        final Kid       kid          = aKid(parent, null);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(kid, parent, newClassroom));
    }

    @Test
    void rejectsParentEnrollingTheirOwnKidIntoAClassroom() {
        final UUID parentId = randomUUID();
        authenticateAs(parentId, "parent");
        final Parent    parent       = aParentWithId(parentId);
        final Classroom newClassroom = aClassroomWithTutor(randomUUID());
        final Kid       kid          = aKid(parent, null);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(kid, parent, newClassroom));
    }

    // --- authorizeDelete ---

    @Test
    void allowsAdminToDeleteAnyKid() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(
                () -> this.policy.authorizeDelete(aKid(aParentWithId(randomUUID()), aClassroomWithTutor(randomUUID()))));
    }

    @Test
    void allowsParentToDeleteTheirOwnKid() {
        final UUID parentId = randomUUID();
        authenticateAs(parentId, "parent");
        final Kid kid = aKid(aParentWithId(parentId), aClassroomWithTutor(randomUUID()));
        assertDoesNotThrow(() -> this.policy.authorizeDelete(kid));
    }

    @Test
    void rejectsParentDeletingAnotherParentsKid() {
        authenticateAs(randomUUID(), "parent");
        final Kid kid = aKid(aParentWithId(randomUUID()), aClassroomWithTutor(randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(kid));
    }

    @Test
    void allowsTutorToDeleteAKidInTheirClassroom() {
        final UUID tutorId = randomUUID();
        authenticateAs(tutorId, "teacher");
        final Kid kid = aKid(aParentWithId(randomUUID()), aClassroomWithTutor(tutorId));
        assertDoesNotThrow(() -> this.policy.authorizeDelete(kid));
    }

    @Test
    void rejectsNonTutorTeacherOfTheClassroomDeletingAKid() {
        authenticateAs(randomUUID(), "teacher");
        final Kid kid = aKid(aParentWithId(randomUUID()), aClassroomWithTutor(randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(kid));
    }

    @Test
    void rejectsDeletingAKidWithNoClassroomAssignedByATeacher() {
        authenticateAs(randomUUID(), "teacher");
        final Kid kid = aKid(aParentWithId(randomUUID()), null);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(kid));
    }

    @Test
    void rejectsDeletingAKidWithNoParentAssignedByAParent() {
        authenticateAs(randomUUID(), "parent");
        final Kid kid = aKid(null, aClassroomWithTutor(randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(kid));
    }

    @Test
    void rejectsDeletingAKidInAClassroomWithNoTutorAssigned() {
        authenticateAs(randomUUID(), "teacher");
        //@formatter:off
        final Classroom classroom = of(Classroom.class).set(field(Classroom.class, "tutor"), (Teacher) null)
                                                       .ignore(field(Classroom.class, "teachers"))
                                                       .ignore(field(Classroom.class, "kids"))
                                                       .create();
        //@formatter:on
        final Kid kid = aKid(aParentWithId(randomUUID()), classroom);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(kid));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final Kid kid = aKid(aParentWithId(randomUUID()), aClassroomWithTutor(randomUUID()));
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(kid));
    }

}
