package edu.lyra.members.api.classroom.handlers;

import edu.lyra.members.api.exceptions.SchoolMismatchException;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import lombok.experimental.UtilityClass;

import static java.util.Objects.isNull;

@UtilityClass
class TeacherSchoolMembership {

    void verifyBelongsToSchool(final School school, final Teacher teacher) {
        if(! (isNull(teacher) || isNull(school) || isSameSchool(school, teacher))) {
            throw new SchoolMismatchException(
                    "Teacher %s does not belong to the classroom's school".formatted(teacher.getId()));
        }
    }

    private boolean isSameSchool(final School school, final Teacher teacher) {
        return school.getId().equals(teacher.getSchool().getId());
    }

}
