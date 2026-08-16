package edu.lyra.members.api.classroom.rest;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.ClassroomHasKidsException;
import edu.lyra.members.api.teacher.Teacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
class ClassroomPolicy {

    void authorizeUpdate(final Classroom classroom) {
        log.debug("Authorizing update of classroom {}", classroom.getId());
        if(this.isNotAdminNorTutor(classroom)) {
            throw new AccessDeniedException("Authenticated user cannot update this classroom");
        }
    }

    private boolean isNotAdminNorTutor(final Classroom classroom) {
        return ! (AuthenticatedPrincipal.isAdmin() || this.isTutor(classroom));
    }

    private boolean isTutor(final Classroom classroom) {
        final Teacher tutor = classroom.getTutor();
        return tutor != null && AuthenticatedPrincipal.isSelf("teacher", tutor.getId());
    }

    void authorizeDelete(final Classroom classroom) {
        log.debug("Authorizing deletion of classroom {}", classroom.getId());
        if(this.isNotAdminNorTutor(classroom)) {
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

}
