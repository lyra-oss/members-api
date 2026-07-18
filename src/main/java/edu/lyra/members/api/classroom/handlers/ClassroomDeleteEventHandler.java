package edu.lyra.members.api.classroom.handlers;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.ClassroomHasKidsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class ClassroomDeleteEventHandler {

    @HandleBeforeDelete
    public void authorizeClassroomDelete(final Classroom classroom) {
        log.debug("Authorizing deletion of classroom {}", classroom.getId());
        if(! (AuthenticatedPrincipal.isAdmin() || this.isTutor(classroom))) {
            throw new AccessDeniedException("Authenticated user cannot delete this classroom");
        }
        if(! classroom.getKids().isEmpty()) {
            //@formatter:off
            throw new ClassroomHasKidsException(
                    "Classroom %s still has %d kid(s) enrolled; move or remove them before deleting this classroom"
                            .formatted(classroom.getId(), classroom.getKids().size()));
            //@formatter:on
        }
    }

    private boolean isTutor(final Classroom classroom) {
        if(classroom.getTutor() == null) {
            return false;
        }
        return AuthenticatedPrincipal.isSelf("teacher", classroom.getTutor().getId());
    }

}
