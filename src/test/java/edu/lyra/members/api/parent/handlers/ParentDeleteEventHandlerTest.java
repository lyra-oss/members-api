package edu.lyra.members.api.parent.handlers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.exceptions.ParentHasKidsException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.person.PersonRole;
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

class ParentDeleteEventHandlerTest {

    private final ParentDeleteEventHandler handler = new ParentDeleteEventHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToDeleteAChildlessParent() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.handler.authorizeParentDelete(aParentWithId(randomUUID())));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Parent aParentWithId(final UUID id) {
        return of(Parent.class).set(field(PersonRole.class, "id"), id).ignore(field(Parent.class, "kids")).create();
    }

    @Test
    void rejectsAdminDeletingAParentThatStillHasKids() {
        authenticateAs(randomUUID(), "admin");
        final Parent parent = aParentWithKids(randomUUID());
        assertThrows(ParentHasKidsException.class, () -> this.handler.authorizeParentDelete(parent));
    }

    private static Parent aParentWithKids(final UUID id) {
        return of(Parent.class).set(field(PersonRole.class, "id"), id)
                               .set(field(Parent.class, "kids"), Set.of(of(Kid.class).create())).create();
    }

    @Test
    void allowsAParentToDeleteTheirOwnChildlessAccount() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        assertDoesNotThrow(() -> this.handler.authorizeParentDelete(aParentWithId(id)));
    }

    @Test
    void rejectsAParentDeletingTheirOwnAccountWhileTheyStillHaveKids() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Parent parent = aParentWithKids(id);
        assertThrows(ParentHasKidsException.class, () -> this.handler.authorizeParentDelete(parent));
    }

    @Test
    void rejectsParentDeletingAnotherParentsAccount() {
        authenticateAs(randomUUID(), "parent");
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeParentDelete(parent));
    }

    @Test
    void rejectsTeacherDeletingAParent() {
        authenticateAs(randomUUID(), "teacher");
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeParentDelete(parent));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeParentDelete(parent));
    }

}
