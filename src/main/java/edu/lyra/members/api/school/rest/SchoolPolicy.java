package edu.lyra.members.api.school.rest;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.SchoolHasReferencesException;
import edu.lyra.members.api.school.School;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
class SchoolPolicy {

    void authorizeUpdate(final School school) {
        log.debug("Authorizing update of school {}", school.getId());
        if(! AuthenticatedPrincipal.isAdmin()) {
            throw new AccessDeniedException("Authenticated user cannot update this school");
        }
    }

    void authorizeDelete(final School school) {
        log.debug("Authorizing deletion of school {}", school.getId());
        if(! AuthenticatedPrincipal.isAdmin()) {
            throw new AccessDeniedException("Authenticated user cannot delete this school");
        }
        if(! school.getClassrooms().isEmpty() || ! school.getTeachers().isEmpty()) {
            //@formatter:off
            throw new SchoolHasReferencesException(
                    ("School %s still has %d classroom(s) and %d teacher(s) linked; remove them before deleting this " +
                     "school").formatted(school.getId(), school.getClassrooms().size(), school.getTeachers().size()));
            //@formatter:on
        }
    }

}
