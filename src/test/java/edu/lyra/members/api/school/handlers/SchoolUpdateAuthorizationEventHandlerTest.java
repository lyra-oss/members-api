package edu.lyra.members.api.school.handlers;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.school.School;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Arrays.stream;

import static org.instancio.Instancio.create;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchoolUpdateAuthorizationEventHandlerTest {

    private final SchoolUpdateAuthorizationEventHandler handler = new SchoolUpdateAuthorizationEventHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToUpdateAnySchool() {
        authenticateAs(UUID.randomUUID(), "admin");
        assertDoesNotThrow(() -> this.handler.authorizeSchoolUpdate(create(School.class)));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void rejectsParentUpdatingASchool() {
        authenticateAs(UUID.randomUUID(), "parent");
        final School school = create(School.class);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeSchoolUpdate(school));
    }

    @Test
    void rejectsTeacherUpdatingASchool() {
        authenticateAs(UUID.randomUUID(), "teacher");
        final School school = create(School.class);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeSchoolUpdate(school));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final School school = create(School.class);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeSchoolUpdate(school));
    }

}
