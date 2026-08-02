package edu.lyra.members.api.parent.rest;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.ParentHasKidsException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
class ParentPolicy {

    void authorizeUpdate(final Parent parent) {
        log.debug("Authorizing update of parent {}", parent.getId());
        if(! this.isAdminOrSelf(parent)) {
            throw new AccessDeniedException("Authenticated user cannot update this parent");
        }
    }

    void authorizeDelete(final Parent parent) {
        log.debug("Authorizing deletion of parent {}", parent.getId());
        if(! this.isAdminOrSelf(parent)) {
            throw new AccessDeniedException("Authenticated user cannot delete this parent");
        }
        if(! parent.getKids().isEmpty()) {
            //@formatter:off
            throw new ParentHasKidsException(
                    "Parent %s still has %d kid(s) linked; remove or reassign them before deleting this parent"
                            .formatted(parent.getId(), parent.getKids().size()));
            //@formatter:on
        }
    }

    void authorizeKidBinding(final Parent parent, final Kid kid) {
        log.debug("Authorizing binding of kid {} to parent {}", kid.getId(), parent.getId());
        if(AuthenticatedPrincipal.isAdmin()) {
            return;
        }
        if(this.isSelf(parent) && this.wasCreatedByCurrentPrincipal(kid)) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot bind this kid to this parent");
    }

    private boolean wasCreatedByCurrentPrincipal(final Kid kid) {
        return AuthenticatedPrincipal.currentId().map(id -> id.toString().equals(kid.getCreatedBy()))
                                     .orElse(false);
    }

    private boolean isAdminOrSelf(final Parent parent) {
        return AuthenticatedPrincipal.isAdmin() || this.isSelf(parent);
    }

    private boolean isSelf(final Parent parent) {
        return AuthenticatedPrincipal.isSelf("parent", parent.getId());
    }

}
