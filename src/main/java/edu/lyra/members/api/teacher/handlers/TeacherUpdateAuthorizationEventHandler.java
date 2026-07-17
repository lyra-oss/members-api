package edu.lyra.members.api.teacher.handlers;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.teacher.Teacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class TeacherUpdateAuthorizationEventHandler {

    @HandleBeforeSave
    public void authorizeTeacherUpdate(final Teacher teacher) {
        log.debug("Authorizing update of teacher {}", teacher.getId());
        final boolean isAdmin = AuthenticatedPrincipal.hasRole("admin");
        final boolean isSelf = AuthenticatedPrincipal.hasRole("teacher") &&
                               AuthenticatedPrincipal.currentId().map(id -> id.equals(teacher.getId())).orElse(false);
        if(! (isAdmin || isSelf)) {
            throw new AccessDeniedException("Authenticated user cannot update this teacher");
        }
    }

}
