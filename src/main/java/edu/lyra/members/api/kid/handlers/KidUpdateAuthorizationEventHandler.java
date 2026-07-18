package edu.lyra.members.api.kid.handlers;

import java.util.Objects;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class KidUpdateAuthorizationEventHandler {

    @HandleBeforeSave
    public void authorizeKidUpdate(final Kid kid) {
        log.debug("Authorizing update of kid {}", kid.getId());
        if(AuthenticatedPrincipal.isAdmin()) {
            return;
        }
        if(! Objects.equals(kid.getPreviousParentId(), id(kid.getParent()))) {
            throw new AccessDeniedException("Authenticated user cannot bind this kid to a different parent");
        }
        if(! Objects.equals(kid.getPreviousClassroomId(), id(kid.getClassroom()))) {
            if(this.isTutorOf(kid.getClassroom())) {
                return;
            }
            throw new AccessDeniedException("Authenticated user cannot enroll this kid into this classroom");
        }
        if(this.isOwnKid(kid) || this.isTutorOf(kid.getClassroom())) {
            return;
        }
        throw new AccessDeniedException("Authenticated user cannot update this kid");
    }

    private static UUID id(final Parent parent) {
        return parent == null ? null : parent.getId();
    }

    private static UUID id(final Classroom classroom) {
        return classroom == null ? null : classroom.getId();
    }

    private boolean isTutorOf(final Classroom classroom) {
        if(classroom == null || classroom.getTutor() == null) {
            return false;
        }
        return AuthenticatedPrincipal.isSelf("teacher", classroom.getTutor().getId());
    }

    private boolean isOwnKid(final Kid kid) {
        return AuthenticatedPrincipal.isSelf("parent", id(kid.getParent()));
    }

}
