package edu.lyra.members.api.classroom.handlers;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
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

import static org.instancio.Instancio.of;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassroomUpdateAuthorizationEventHandlerTest {

    private final ClassroomUpdateAuthorizationEventHandler handler = new ClassroomUpdateAuthorizationEventHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToUpdateAnyClassroom() {
        authenticateAs(UUID.randomUUID(), "admin");
        final Classroom classroom = aClassroomWithPreviousTutor(UUID.randomUUID());
        assertDoesNotThrow(() -> this.handler.authorizeClassroomUpdate(classroom));
        assertDoesNotThrow(() -> this.handler.authorizeClassroomLinkUpdate(classroom, ""));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Classroom aClassroomWithPreviousTutor(final UUID tutorId) {
        //@formatter:off
        return of(Classroom.class)
                        .set(field(Classroom.class, "previousTutorId"), tutorId)
                        .ignore(field(Classroom.class, "teachers"))
                        .ignore(field(Classroom.class, "kids"))
                        .create();
        //@formatter:on
    }

    @Test
    void allowsPreviousTutorToUpdateTheClassroom() {
        final UUID tutorId = UUID.randomUUID();
        authenticateAs(tutorId, "teacher");
        assertDoesNotThrow(() -> this.handler.authorizeClassroomUpdate(aClassroomWithPreviousTutor(tutorId)));
    }

    @Test
    void allowsPreviousTutorToUpdateClassroomLinks() {
        final UUID tutorId = UUID.randomUUID();
        authenticateAs(tutorId, "teacher");
        assertDoesNotThrow(() -> this.handler.authorizeClassroomLinkUpdate(aClassroomWithPreviousTutor(tutorId), ""));
    }

    @Test
    void rejectsNonTutorTeacherUpdatingTheClassroom() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Classroom classroom = aClassroomWithPreviousTutor(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeClassroomUpdate(classroom));
    }

    @Test
    void rejectsUpdatingAClassroomWithNoPreviousTutor() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Classroom classroom = aClassroomWithPreviousTutor(null);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeClassroomUpdate(classroom));
    }

    @Test
    void rejectsParentUpdatingAClassroom() {
        authenticateAs(UUID.randomUUID(), "parent");
        final Classroom classroom = aClassroomWithPreviousTutor(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeClassroomUpdate(classroom));
    }

    @Test
    void authorizesTutorReassignmentAgainstTheOutgoingTutorNotTheProposedOne() {
        final UUID outgoingTutorId = UUID.randomUUID();
        authenticateAs(outgoingTutorId, "teacher");
        final Classroom proposed = of(Classroom.class).set(field(Classroom.class, "tutor"), aTeacher(UUID.randomUUID()))
                                                      .set(field(Classroom.class, "previousTutorId"), outgoingTutorId)
                                                      .ignore(field(Classroom.class, "teachers"))
                                                      .ignore(field(Classroom.class, "kids")).create();
        assertDoesNotThrow(() -> this.handler.authorizeClassroomLinkUpdate(proposed, ""));
    }

    private static Teacher aTeacher(final UUID id) {
        return of(Teacher.class).set(field(PersonRole.class, "id"), id).create();
    }

    @Test
    void rejectsProposedNewTutorReassigningThemselvesBeforeTakingOver() {
        final UUID outgoingTutorId = UUID.randomUUID();
        final UUID proposedTutorId = UUID.randomUUID();
        authenticateAs(proposedTutorId, "teacher");
        final Classroom proposed = of(Classroom.class).set(field(Classroom.class, "tutor"), aTeacher(proposedTutorId))
                                                      .set(field(Classroom.class, "previousTutorId"), outgoingTutorId)
                                                      .ignore(field(Classroom.class, "teachers"))
                                                      .ignore(field(Classroom.class, "kids")).create();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeClassroomLinkUpdate(proposed, ""));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Classroom classroom = aClassroomWithPreviousTutor(UUID.randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeClassroomUpdate(classroom));
    }

}
