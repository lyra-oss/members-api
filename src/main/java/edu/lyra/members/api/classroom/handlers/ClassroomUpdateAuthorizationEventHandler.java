package edu.lyra.members.api.classroom.handlers;

import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class ClassroomUpdateAuthorizationEventHandler {

    @HandleBeforeSave
    public void authorizeClassroomUpdate(final Classroom classroom) {
        log.debug("Authorizing update of classroom {}", classroom.getId());
        this.authorize(classroom);
    }

    private void authorize(final Classroom classroom) {
        if(AuthenticatedPrincipal.hasRole("admin") || this.isPreviousTutor(classroom)) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot update this classroom");
    }

    private boolean isPreviousTutor(final Classroom classroom) {
        final UUID previousTutorId = classroom.getPreviousTutorId();
        if(previousTutorId == null) {
            return false;
        }
        return AuthenticatedPrincipal.hasRole("teacher") &&
               AuthenticatedPrincipal.currentId().map(previousTutorId::equals).orElse(false);
    }

    @HandleBeforeLinkSave
    public void authorizeClassroomLinkUpdate(final Classroom classroom, final Object linked) {
        log.debug("Authorizing link update of classroom {}", classroom.getId());
        this.authorize(classroom);
    }

}
