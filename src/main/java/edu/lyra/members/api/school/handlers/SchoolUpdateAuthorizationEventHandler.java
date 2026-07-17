package edu.lyra.members.api.school.handlers;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.school.School;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class SchoolUpdateAuthorizationEventHandler {

    @HandleBeforeSave
    public void authorizeSchoolUpdate(final School school) {
        log.debug("Authorizing update of school {}", school.getId());
        if(! AuthenticatedPrincipal.hasRole("admin")) {
            throw new AccessDeniedException("Authenticated user cannot update this school");
        }
    }

}
