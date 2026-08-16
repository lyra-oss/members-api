package edu.lyra.members.api.teacher.rest;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.TeacherAssignedToClassroomException;
import edu.lyra.members.api.teacher.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RequiredArgsConstructor
class TeacherPolicy {

    private final ClassroomRepository classroomRepository;

    void authorizeUpdate(final Teacher teacher) {
        log.debug("Authorizing update of teacher {}", teacher.getId());
        if(this.isNotAdminNorSelf(teacher)) {
            throw new AccessDeniedException("Authenticated user cannot update this teacher");
        }
    }

    private boolean isNotAdminNorSelf(final Teacher teacher) {
        return ! (AuthenticatedPrincipal.isAdmin() || AuthenticatedPrincipal.isSelf("teacher", teacher.getId()));
    }

    void authorizeDelete(final Teacher teacher) {
        log.debug("Authorizing deletion of teacher {}", teacher.getId());
        if(this.isNotAdminNorSelf(teacher)) {
            throw new AccessDeniedException("Authenticated user cannot delete this teacher");
        }
        if(this.classroomRepository.existsByTutorIdOrTeachersId(teacher.getId())) {
            throw new TeacherAssignedToClassroomException(
                    ("Teacher %s still tutors or teaches at least one classroom; unassign them before deleting this " +
                     "teacher").formatted(teacher.getId()));
        }
    }

}
