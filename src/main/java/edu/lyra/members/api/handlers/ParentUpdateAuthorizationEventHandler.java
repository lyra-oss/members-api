package edu.lyra.members.api.handlers;

import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.Parent;
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
        return AuthenticatedPrincipal.hasRole("admin");
    }

    private boolean isSelf(final Parent parent) {
        return AuthenticatedPrincipal.hasRole("parent") &&
               AuthenticatedPrincipal.currentId().map(id -> id.equals(parent.getId())).orElse(false);
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

    private boolean allCreatedByCurrentPrincipal(final Object linked) {
        return AuthenticatedPrincipal.currentId().map(id -> this.matchesCreator(linked, id.toString())).orElse(false);
    }

    private boolean matchesCreator(final Object linked, final String subject) {
        return switch(linked) {
            case Kid kid -> subject.equals(kid.getCreatedBy());
            case Iterable<?> kids -> this.allMatch(kids, subject);
            default -> false;
        };
    }

    private boolean allMatch(final Iterable<?> kids, final String subject) {
        for(final Object candidate : kids) {
            if(! (candidate instanceof Kid kid) || ! subject.equals(kid.getCreatedBy())) {
                return false;
            }
        }
        return true;
    }

}
