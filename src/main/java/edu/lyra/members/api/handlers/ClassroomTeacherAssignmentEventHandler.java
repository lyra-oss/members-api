package edu.lyra.members.api.handlers;

import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.Teacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;

@Slf4j
@RepositoryEventHandler
class ClassroomTeacherAssignmentEventHandler {

    @HandleBeforeCreate
    public void verifyTeachersBelongToSchool(final Classroom classroom) {
        log.debug("Verifying tutor and teachers belong to classroom's school before creation");
        this.verifySameSchool(classroom);
    }

    @HandleBeforeLinkSave
    public void verifyLinkedTeachersBelongToSchool(final Classroom classroom, final Object linked) {
        log.debug("Verifying tutor and teachers belong to classroom's school before link save");
        this.verifySameSchool(classroom);
    }

    private void verifySameSchool(final Classroom classroom) {
        final School classroomSchool = classroom.getSchool();
        if(classroomSchool == null) {
            return;
        }
        this.verifyBelongsToSchool(classroomSchool, classroom.getTutor());
        if(classroom.getTeachers() != null) {
            classroom.getTeachers().forEach(teacher -> this.verifyBelongsToSchool(classroomSchool, teacher));
        }
    }

    private void verifyBelongsToSchool(final School classroomSchool, final Teacher teacher) {
        if(teacher != null && ! classroomSchool.getId().equals(teacher.getSchool().getId())) {
            throw new SchoolMismatchException(
                    "Teacher %s does not belong to the classroom's school".formatted(teacher.getId()));
        }
    }

}
