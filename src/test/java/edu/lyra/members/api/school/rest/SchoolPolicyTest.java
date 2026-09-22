package edu.lyra.members.api.school.rest;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.exceptions.SchoolHasReferencesException;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.TeacherRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolPolicyTest {

    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private TeacherRepository   teacherRepository;

    private SchoolPolicy policy;

    @BeforeEach
    void setUp() {
        this.policy = new SchoolPolicy(this.classroomRepository, this.teacherRepository);
    }

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

    private static School aSchool() {
        return of(School.class).create();
    }

    @Test
    void allowsAdminToUpdateASchool() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aSchool()));
    }

    @Test
    void rejectsParentUpdatingASchool() {
        authenticateAs(randomUUID(), "parent");
        final School school = aSchool();
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(school));
    }

    @Test
    void rejectsTeacherUpdatingASchool() {
        authenticateAs(randomUUID(), "teacher");
        final School school = aSchool();
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(school));
    }

    @Test
    void allowsAdminToDeleteASchoolWithNoReferences() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aSchool()));
    }

    @Test
    void rejectsAdminDeletingASchoolThatStillHasClassrooms() {
        authenticateAs(randomUUID(), "admin");
        final School school = aSchool();
        when(this.classroomRepository.countBySchoolId(school.getId())).thenReturn(1L);
        assertThrows(SchoolHasReferencesException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsAdminDeletingASchoolThatStillHasTeachers() {
        authenticateAs(randomUUID(), "admin");
        final School school = aSchool();
        when(this.teacherRepository.countBySchoolId(school.getId())).thenReturn(1L);
        assertThrows(SchoolHasReferencesException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsParentDeletingASchool() {
        authenticateAs(randomUUID(), "parent");
        final School school = aSchool();
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsTeacherDeletingASchool() {
        authenticateAs(randomUUID(), "teacher");
        final School school = aSchool();
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(school));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final School school = aSchool();
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(school));
    }

}
