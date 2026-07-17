package edu.lyra.members.api.parent.handlers;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParentRegistrationHandlerTest {

    private final ParentRegistrationHandler handler = new ParentRegistrationHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignsJwtSubjectAsParentId() {
        final UUID subject = UUID.randomUUID();
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject(subject.toString())
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        final Parent parent = new Parent();
        this.handler.setSubjectAsId(parent);
        assertEquals(subject, parent.getId());
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
        final Parent parent = new Parent();
        assertThrows(AccessDeniedException.class, () -> this.handler.setSubjectAsId(parent));
    }

    @Test
    void rejectsNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "credentials"));
        final Parent parent = new Parent();
        assertThrows(AccessDeniedException.class, () -> this.handler.setSubjectAsId(parent));
    }

}
