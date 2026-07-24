package edu.lyra.members.api.parent.handlers;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class ParentUpdateAuthorizationEventHandler {

    @HandleBeforeSave
    public void authorizeParentUpdate(final Parent parent) {
        log.debug("Authorizing update of parent {}", parent.getId());
        if(this.isAdmin() || this.isSelf(parent)) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot update this parent");
    }

    private boolean isAdmin() {
        return AuthenticatedPrincipal.isAdmin();
    }

    private boolean isSelf(final Parent parent) {
        return AuthenticatedPrincipal.isSelf("parent", parent.getId());
    }

    @HandleBeforeLinkSave
    public void authorizeKidBinding(final Parent parent, final Object linked) {
        log.debug("Authorizing binding of a kid to parent {}", parent.getId());
        if(this.isAdmin()) {
            return;
        }
        if(this.isSelf(parent) && this.allCreatedByCurrentPrincipal(linked)) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot bind this kid to this parent");
    }

    /*
     * "kids" is a Set<Kid>, a to-many association, so Spring Data REST always resolves the linked side of a
     * @HandleBeforeLinkSave as a Collection<Kid> (a Hibernate-managed PersistentSet, confirmed by direct
     * observation) - never a bare Kid and never any other type. There is deliberately no instanceof guard
     * here: an unexpected element would only ever come from a bug elsewhere, and a ClassCastException still
     * fails the request (and hence the link) just as safely as an explicit denial would.
     */
    private boolean allCreatedByCurrentPrincipal(final Object linked) {
        return AuthenticatedPrincipal.currentId()
                                     .map(id -> this.allCreatedBy((Iterable<?>) linked, id.toString()))
                                     .orElse(false);
    }

    private boolean allCreatedBy(final Iterable<?> kids, final String subject) {
        for(final Object candidate : kids) {
            if(! subject.equals(((Kid) candidate).getCreatedBy())) {
                return false;
            }
        }
        return true;
    }

}
