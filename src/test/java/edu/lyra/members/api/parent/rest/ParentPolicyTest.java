package edu.lyra.members.api.parent.rest;

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
import org.springframework.test.util.ReflectionTestUtils;

import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;

import static org.instancio.Instancio.of;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParentPolicyTest {

    private final ParentPolicy policy = new ParentPolicy();

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

    private static Parent aParentWithId(final UUID id) {
        return of(Parent.class).set(field(PersonRole.class, "id"), id).set(field(Parent.class, "kids"), Set.of())
                                .create();
    }

    private static Kid aKidCreatedBy(final UUID creatorId) {
        final Kid kid = new Kid();
        ReflectionTestUtils.setField(kid, "id", randomUUID());
        ReflectionTestUtils.setField(kid, "createdBy", creatorId.toString());
        return kid;
    }

    @Test
    void allowsAdminToUpdateAnyParent() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aParentWithId(randomUUID())));
    }

    @Test
    void allowsAParentToUpdateTheirOwnAccount() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        assertDoesNotThrow(() -> this.policy.authorizeUpdate(aParentWithId(id)));
    }

    @Test
    void rejectsAParentUpdatingAnotherParentsAccount() {
        authenticateAs(randomUUID(), "parent");
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(parent));
    }

    @Test
    void rejectsATeacherUpdatingAParent() {
        authenticateAs(randomUUID(), "teacher");
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(parent));
    }

    @Test
    void rejectsUnauthenticatedUpdate() {
        SecurityContextHolder.clearContext();
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeUpdate(parent));
    }

    @Test
    void allowsAdminToDeleteAParentWithNoKids() {
        authenticateAs(randomUUID(), "admin");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aParentWithId(randomUUID())));
    }

    @Test
    void allowsAParentToDeleteTheirOwnAccountWhenTheyHaveNoKids() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        assertDoesNotThrow(() -> this.policy.authorizeDelete(aParentWithId(id)));
    }

    @Test
    void rejectsDeletingAParentThatStillHasKids() {
        authenticateAs(randomUUID(), "admin");
        final UUID id = randomUUID();
        //@formatter:off
        final Parent parent = of(Parent.class).set(field(PersonRole.class, "id"), id)
                                               .set(field(Parent.class, "kids"), Set.of(aKidCreatedBy(id)))
                                               .create();
        //@formatter:on
        assertThrows(ParentHasKidsException.class, () -> this.policy.authorizeDelete(parent));
    }

    @Test
    void rejectsAParentDeletingAnotherParentsAccount() {
        authenticateAs(randomUUID(), "parent");
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(parent));
    }

    @Test
    void rejectsATeacherDeletingAParent() {
        authenticateAs(randomUUID(), "teacher");
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(parent));
    }

    @Test
    void rejectsUnauthenticatedDelete() {
        SecurityContextHolder.clearContext();
        final Parent parent = aParentWithId(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeDelete(parent));
    }

    @Test
    void allowsAdminToBindAnyKidToAnyParent() {
        authenticateAs(randomUUID(), "admin");
        final Parent parent = aParentWithId(randomUUID());
        final Kid    kid    = aKidCreatedBy(randomUUID());
        assertDoesNotThrow(() -> this.policy.authorizeKidBinding(parent, kid));
    }

    @Test
    void allowsAParentToBindToThemselvesAKidTheyCreated() {
        final UUID id     = randomUUID();
        final Parent parent = aParentWithId(id);
        final Kid    kid    = aKidCreatedBy(id);
        authenticateAs(id, "parent");
        assertDoesNotThrow(() -> this.policy.authorizeKidBinding(parent, kid));
    }

    @Test
    void rejectsAParentBindingToThemselvesAKidCreatedBySomeoneElse() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Parent parent = aParentWithId(id);
        final Kid    kid    = aKidCreatedBy(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeKidBinding(parent, kid));
    }

    @Test
    void rejectsAParentBindingAKidTheyCreatedToADifferentParentsAccount() {
        final UUID id = randomUUID();
        authenticateAs(id, "parent");
        final Parent parent = aParentWithId(randomUUID());
        final Kid    kid    = aKidCreatedBy(id);
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeKidBinding(parent, kid));
    }

    @Test
    void rejectsATeacherBindingAKidToAParent() {
        authenticateAs(randomUUID(), "teacher");
        final Parent parent = aParentWithId(randomUUID());
        final Kid    kid    = aKidCreatedBy(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeKidBinding(parent, kid));
    }

    @Test
    void rejectsUnauthenticatedKidBinding() {
        SecurityContextHolder.clearContext();
        final Parent parent = aParentWithId(randomUUID());
        final Kid    kid    = aKidCreatedBy(randomUUID());
        assertThrows(AccessDeniedException.class, () -> this.policy.authorizeKidBinding(parent, kid));
    }

}
