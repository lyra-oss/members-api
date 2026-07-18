package edu.lyra.members.api.teacher.handlers;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.TeacherAssignedToClassroomException;
import edu.lyra.members.api.teacher.Teacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class TeacherDeleteEventHandler {

    private final ClassroomRepository classroomRepository;

    TeacherDeleteEventHandler(final ClassroomRepository classroomRepository) {
        this.classroomRepository = classroomRepository;
    }

    @HandleBeforeDelete
    public void authorizeTeacherDelete(final Teacher teacher) {
        log.debug("Authorizing deletion of teacher {}", teacher.getId());
        final boolean isAdmin = AuthenticatedPrincipal.isAdmin();
        final boolean isSelf = AuthenticatedPrincipal.isSelf("teacher", teacher.getId());
        if(! (isAdmin || isSelf)) {
            throw new AccessDeniedException("Authenticated user cannot delete this teacher");
        }
        if(this.classroomRepository.existsByTutorIdOrTeachersId(teacher.getId())) {
            throw new TeacherAssignedToClassroomException(
                    ("Teacher %s still tutors or teaches at least one classroom; unassign them before deleting this " +
                     "teacher").formatted(teacher.getId()));
        }
    }

}
