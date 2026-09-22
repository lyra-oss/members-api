package edu.lyra.members.api.school.rest;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.SchoolHasReferencesException;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RequiredArgsConstructor
class SchoolPolicy {

    private final ClassroomRepository classroomRepository;
    private final TeacherRepository   teacherRepository;

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
        final long classrooms = this.classroomRepository.countBySchoolId(school.getId());
        final long teachers   = this.teacherRepository.countBySchoolId(school.getId());
        if(classrooms > 0 || teachers > 0) {
            //@formatter:off
            throw new SchoolHasReferencesException(
                    ("School %s still has %d classroom(s) and %d teacher(s) linked; remove them before deleting this " +
                     "school").formatted(school.getId(), classrooms, teachers));
            //@formatter:on
        }
    }

}
