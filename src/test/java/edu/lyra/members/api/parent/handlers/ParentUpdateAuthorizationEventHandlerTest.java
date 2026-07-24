package edu.lyra.members.api.parent.handlers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.config.jpa.Auditable;
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

import static java.util.UUID.randomUUID;

import static org.instancio.Instancio.of;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParentUpdateAuthorizationEventHandlerTest {

    private final ParentUpdateAuthorizationEventHandler handler = new ParentUpdateAuthorizationEventHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsAdminToUpdateAnyParent() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.handler.authorizeParentUpdate(aParent()));
    }

    private static void authenticateAs(final UUID id, final String... roles) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        final Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Parent aParent() {
        return aParentWithId(randomUUID());
    }

    private static Parent aParentWithId(final UUID id) {
        return of(Parent.class).set(field(PersonRole.class, "id"), id).ignore(field(Parent.class, "kids")).create();
    }

    @Test
    void allowsParentToUpdateOwnAccount() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        assertDoesNotThrow(() -> this.handler.authorizeParentUpdate(aParentWithId(id)));
    }

    @Test
    void rejectsParentUpdatingAnotherParent() {
        authenticateAs(randomUUID(), "parent");
        final Parent parent = aParent();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeParentUpdate(parent));
    }

    @Test
    void rejectsTeacherUpdatingAParent() {
        authenticateAs(randomUUID(), "teacher");
        final Parent parent = aParent();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeParentUpdate(parent));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Parent parent = aParent();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeParentUpdate(parent));
    }

    /*
     * Spring Data REST always resolves the "kids" association's linked side as a Collection<Kid> - never a
     * bare Kid - since "kids" is a Set<Kid> (see the production code's comment on
     * allCreatedByCurrentPrincipal, confirmed by direct observation of a real @HandleBeforeLinkSave
     * invocation). Every scenario below binds through a Set, matching that real contract.
     */
    @Test
    void allowsAdminToBindAnyKid() {
        authenticateAs(randomUUID(), "admin");
        final Kid kid = aKidCreatedBy(randomUUID().toString());
        assertDoesNotThrow(() -> this.handler.authorizeKidBinding(aParent(), Set.of(kid)));
    }

    private static Kid aKidCreatedBy(final String createdBy) {
        return of(Kid.class).set(field(Auditable.class, "createdBy"), createdBy).create();
    }

    @Test
    void allowsParentToBindAKidTheyCreatedToThemselves() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Kid kid = aKidCreatedBy(id.toString());
        assertDoesNotThrow(() -> this.handler.authorizeKidBinding(aParentWithId(id), Set.of(kid)));
    }

    @Test
    void allowsParentToBindMultipleKidsTheyCreatedToThemselves() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Set<Kid> kids = Set.of(aKidCreatedBy(id.toString()), aKidCreatedBy(id.toString()));
        assertDoesNotThrow(() -> this.handler.authorizeKidBinding(aParentWithId(id), kids));
    }

    @Test
    void rejectsParentBindingAKidTheyDidNotCreate() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Kid    kid    = aKidCreatedBy(randomUUID().toString());
        final Parent parent = aParentWithId(id);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidBinding(parent, Set.of(kid)));
    }

    @Test
    void rejectsParentBindingAKidTheyCreatedToADifferentParentAccount() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Kid    kid    = aKidCreatedBy(id.toString());
        final Parent parent = aParent();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidBinding(parent, Set.of(kid)));
    }

    @Test
    void rejectsParentBindingWhenOneOfSeveralKidsWasNotCreatedByThem() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Set<Kid> kids   = Set.of(aKidCreatedBy(id.toString()), aKidCreatedBy(randomUUID().toString()));
        final Parent   parent = aParentWithId(id);
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidBinding(parent, kids));
    }

    @Test
    void rejectsTeacherBindingAKidToAParent() {
        authenticateAs(randomUUID(), "teacher");
        final Kid    kid    = aKidCreatedBy(randomUUID().toString());
        final Parent parent = aParent();
        assertThrows(AccessDeniedException.class, () -> this.handler.authorizeKidBinding(parent, Set.of(kid)));
    }

}
