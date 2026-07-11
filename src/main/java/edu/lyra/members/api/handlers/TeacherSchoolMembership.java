package edu.lyra.members.api.handlers;

import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.Teacher;

public final class TeacherSchoolMembership {

    private TeacherSchoolMembership() {
    }

    public static void verifyBelongsToSchool(final School classroomSchool, final Teacher teacher) {
        if(teacher != null && classroomSchool != null
                && ! classroomSchool.getId().equals(teacher.getSchool().getId())) {
            throw new SchoolMismatchException(
                    "Teacher %s does not belong to the classroom's school".formatted(teacher.getId()));
        }
    }

}
