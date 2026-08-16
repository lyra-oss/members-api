package edu.lyra.members.api.school.rest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.exceptions.SchoolHasReferencesException;
import edu.lyra.members.api.school.School;
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

class SchoolPolicyTest {

    private final SchoolPolicy policy = new SchoolPolicy();

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

    private static School aSchool(final Set<Classroom> classrooms, final Set<Teacher> teachers) {
        //@formatter:off
        return of(School.class).set(field(School.class, "classrooms"), classrooms)
                               .set(field(School.class, "teachers"), teachers)
                               .create();
        //@formatter:on
    }

    @Test
    void allowsAdminToUpdateASchool() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aSchool(Set.of(), Set.of())));
    }

    @Test
    void rejectsParentUpdatingASchool() {
        authenticateAs(randomUUID(), "parent");
        final School school = aSchool(Set.of(), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(school));
    }

    @Test
    void rejectsTeacherUpdatingASchool() {
        authenticateAs(randomUUID(), "teacher");
        final School school = aSchool(Set.of(), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(school));
    }

    @Test
    void allowsAdminToDeleteASchoolWithNoReferences() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aSchool(Set.of(), Set.of())));
    }

    @Test
    void rejectsAdminDeletingASchoolThatStillHasClassrooms() {
        authenticateAs(randomUUID(), "admin");
        final School school = aSchool(Set.of(of(Classroom.class).create()), Set.of());
        assertThrows(SchoolHasReferencesException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsAdminDeletingASchoolThatStillHasTeachers() {
        authenticateAs(randomUUID(), "admin");
        final School school = aSchool(Set.of(), Set.of(of(Teacher.class).create()));
        assertThrows(SchoolHasReferencesException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsParentDeletingASchool() {
        authenticateAs(randomUUID(), "parent");
        final School school = aSchool(Set.of(), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsTeacherDeletingASchool() {
        authenticateAs(randomUUID(), "teacher");
        final School school = aSchool(Set.of(), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final School school = aSchool(Set.of(), Set.of());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(school));
    }

}
