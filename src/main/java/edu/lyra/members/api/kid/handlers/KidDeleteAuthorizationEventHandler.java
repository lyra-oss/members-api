package edu.lyra.members.api.kid.handlers;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class KidDeleteAuthorizationEventHandler {

    @HandleBeforeDelete
    public void authorizeKidDelete(final Kid kid) {
        log.debug("Authorizing deletion of kid {}", kid.getId());
        if(AuthenticatedPrincipal.hasRole("admin") || this.isOwnKid(kid) || this.isTutorOf(kid.getClassroom())) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot delete this kid");
    }

    private boolean isOwnKid(final Kid kid) {
        final Parent parent = kid.getParent();
        return AuthenticatedPrincipal.hasRole("parent") && parent != null &&
               AuthenticatedPrincipal.currentId().map(current -> current.equals(parent.getId())).orElse(false);
    }

    private boolean isTutorOf(final Classroom classroom) {
        if(classroom == null || classroom.getTutor() == null) {
            return false;
        }
        return AuthenticatedPrincipal.hasRole("teacher") &&
               AuthenticatedPrincipal.currentId().map(current -> current.equals(classroom.getTutor().getId()))
                                     .orElse(false);
    }

}
