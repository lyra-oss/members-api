package edu.lyra.members.api.school.handlers;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.SchoolHasReferencesException;
import edu.lyra.members.api.school.School;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class SchoolDeleteEventHandler {

    @HandleBeforeDelete
    public void authorizeSchoolDelete(final School school) {
        log.debug("Authorizing deletion of school {}", school.getId());
        if(! AuthenticatedPrincipal.hasRole("admin")) {
            throw new AccessDeniedException("Authenticated user cannot delete this school");
        }
        if(! school.getClassrooms().isEmpty() || ! school.getTeachers().isEmpty()) {
            throw new SchoolHasReferencesException(
                    ("School %s still has %d classroom(s) and %d teacher(s) linked; remove them before deleting this " +
                     "school").formatted(school.getId(), school.getClassrooms().size(), school.getTeachers().size()));
        }
    }

}
