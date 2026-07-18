package edu.lyra.members.api.parent.handlers;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.ParentHasKidsException;
import edu.lyra.members.api.parent.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class ParentDeleteEventHandler {

    @HandleBeforeDelete
    public void authorizeParentDelete(final Parent parent) {
        log.debug("Authorizing deletion of parent {}", parent.getId());
        final boolean isAdmin = AuthenticatedPrincipal.isAdmin();
        final boolean isSelf = AuthenticatedPrincipal.isSelf("parent", parent.getId());
        if(! (isAdmin || isSelf)) {
            throw new AccessDeniedException("Authenticated user cannot delete this parent");
        }
        if(! parent.getKids().isEmpty()) {
            throw new ParentHasKidsException(
                    "Parent %s still has %d kid(s) linked; remove or reassign them before deleting this parent".formatted(
                            parent.getId(), parent.getKids().size()));
        }
    }

}
