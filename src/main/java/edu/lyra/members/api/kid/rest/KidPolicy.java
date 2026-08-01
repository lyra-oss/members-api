package edu.lyra.members.api.kid.rest;

import java.util.Objects;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
class KidPolicy {

    // kid is the pre-write kid (its parent/classroom are the current, "outgoing" values); newParent/
    // newClassroom are the proposed values the adapter already resolved (equal to the current ones
    // when the request left that field unspecified).
    void authorizeUpdate(final Kid kid, final Parent newParent, final Classroom newClassroom) {
        log.debug("Authorizing update of kid {}", kid.getId());
        if(AuthenticatedPrincipal.isAdmin()) {
            return;
        }
        if(! Objects.equals(id(kid.getParent()), id(newParent))) {
            throw new AccessDeniedException("Authenticated user cannot bind this kid to a different parent");
        }
        if(! Objects.equals(id(kid.getClassroom()), id(newClassroom))) {
            if(this.isTutorOf(newClassroom)) {
                return;
            }
            throw new AccessDeniedException("Authenticated user cannot enroll this kid into this classroom");
        }
        if(this.isOwnKid(kid) || this.isTutorOf(kid.getClassroom())) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot update this kid");
    }

    void authorizeDelete(final Kid kid) {
        log.debug("Authorizing deletion of kid {}", kid.getId());
        if(AuthenticatedPrincipal.isAdmin() || this.isOwnKid(kid) || this.isTutorOf(kid.getClassroom())) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot delete this kid");
    }

    private static UUID id(final Parent parent) {
        return parent == null ? null : parent.getId();
    }

    private static UUID id(final Classroom classroom) {
        return classroom == null ? null : classroom.getId();
    }

    private boolean isTutorOf(final Classroom classroom) {
        if(classroom == null || classroom.getTutor() == null) {
            return false;
        }
        return AuthenticatedPrincipal.isSelf("teacher", classroom.getTutor().getId());
    }

    private boolean isOwnKid(final Kid kid) {
        final Parent parent = kid.getParent();
        return parent != null && AuthenticatedPrincipal.isSelf("parent", parent.getId());
    }

}
