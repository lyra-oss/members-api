package edu.lyra.members.api.handlers;

import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.School;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;

@Slf4j
@RepositoryEventHandler
class ClassroomTeacherAssignmentEventHandler {

    @HandleBeforeCreate
    public void verifyTeachersBelongToSchool(final Classroom classroom) {
        log.debug("Verifying tutor and teachers belong to classroom's school before creation");
        final School classroomSchool = classroom.getSchool();
        TeacherSchoolMembership.verifyBelongsToSchool(classroomSchool, classroom.getTutor());
        if(classroom.getTeachers() != null) {
            classroom.getTeachers()
                     .forEach(teacher -> TeacherSchoolMembership.verifyBelongsToSchool(classroomSchool, teacher));
        }
    }

}
