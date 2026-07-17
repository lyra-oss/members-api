package edu.lyra.members.api.teacher.handlers;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.teacher.Teacher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeacherRegistrationHandlerTest {

    private final TeacherRegistrationHandler handler = new TeacherRegistrationHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignsJwtSubjectAsTeacherId() {
        final UUID subject = UUID.randomUUID();
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject(subject.toString())
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Teacher teacher = new Teacher();
        this.handler.setSubjectAsId(teacher);
        assertEquals(subject, teacher.getId());
    }

    @Test
    void rejectsMissingSubjectClaim() {
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .claim("preferred_username", "john")
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Teacher teacher = new Teacher();
        assertThrows(AccessDeniedException.class, () -> this.handler.setSubjectAsId(teacher));
    }

    @Test
    void rejectsNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "credentials"));
        final Teacher teacher = new Teacher();
        assertThrows(AccessDeniedException.class, () -> this.handler.setSubjectAsId(teacher));
    }

}
