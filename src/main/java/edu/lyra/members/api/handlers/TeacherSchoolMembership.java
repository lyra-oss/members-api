package edu.lyra.members.api.handlers;

import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.Teacher;
import lombok.experimental.UtilityClass;

import static java.util.Objects.isNull;

@UtilityClass
public class TeacherSchoolMembership {

    public void verifyBelongsToSchool(final School school, final Teacher teacher) {
        if(! (isNull(teacher) || isNull(school) || isSameSchool(school, teacher))) {
            throw new SchoolMismatchException(
                    "Teacher %s does not belong to the classroom's school".formatted(teacher.getId()));
        }
    }

    private boolean isSameSchool(final School school, final Teacher teacher) {
        return school.getId().equals(teacher.getSchool().getId());
    }

}
