package edu.lyra.members.api.handlers;

import java.util.stream.Stream;

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
        final Stream<Teacher> tutor        = Stream.ofNullable(classroom.getTutor());
        final Stream<Teacher> otherTeachers =
                classroom.getTeachers() == null ? Stream.empty() : classroom.getTeachers().stream();
        Stream.concat(tutor, otherTeachers)
              .filter(teacher -> ! teacher.belongsToSchool(classroomSchool)).findFirst()
              .ifPresent(teacher -> {
                  throw new SchoolMismatchException(
                          "Teacher %s does not belong to the classroom's school".formatted(teacher.getId()));
              });
    }

}
