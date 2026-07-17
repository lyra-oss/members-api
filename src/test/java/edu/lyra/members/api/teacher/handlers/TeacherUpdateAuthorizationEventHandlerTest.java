package edu.lyra.members.api.teacher.handlers;

import java.util.List;
import java.util.UUID;

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

class TeacherUpdateAuthorizationEventHandlerTest {

    private final TeacherUpdateAuthorizationEventHandler handler = new TeacherUpdateAuthorizationEventHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToUpdateAnyTeacher() {
        authenticateAs(UUID.randomUUID(), "admin");
        assertDoesNotThrow(() -> this.handler.authorizeTeacherUpdate(aTeacher()));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Teacher aTeacher() {
        return aTeacherWithId(UUID.randomUUID());
    }

    private static Teacher aTeacherWithId(final UUID id) {
        return Instancio.of(Teacher.class).set(field(Teacher.class, "id"), id).create();
    }

    @Test
    void allowsTeacherToUpdateOwnAccount() {
        final UUID id = UUID.randomUUID();
        authenticateAs(id, "teacher");
        assertDoesNotThrow(() -> this.handler.authorizeTeacherUpdate(aTeacherWithId(id)));
    }

    @Test
    void rejectsTeacherUpdatingAnotherTeachersAccount() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final Teacher teacher = aTeacher();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeTeacherUpdate(teacher));
    }

    @Test
    void rejectsParentUpdatingATeacher() {
        authenticateAs(UUID.randomUUID(), "parent");
        final Teacher teacher = aTeacher();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeTeacherUpdate(teacher));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Teacher teacher = aTeacher();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeTeacherUpdate(teacher));
    }

}
