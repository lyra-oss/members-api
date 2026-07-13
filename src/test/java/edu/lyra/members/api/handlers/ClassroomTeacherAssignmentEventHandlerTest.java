package edu.lyra.members.api.handlers;

import java.util.Set;

import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.Teacher;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassroomTeacherAssignmentEventHandlerTest {

    private final ClassroomTeacherAssignmentEventHandler handler = new ClassroomTeacherAssignmentEventHandler();

    @Test
    void allowsCreatingClassroomWithoutTutorOrTeachers() {
        final Classroom classroom = classroomWith(aSchool(), null, Set.of());
        assertDoesNotThrow(() -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void allowsCreatingClassroomWhenSchoolIsNotYetSet() {
        final Classroom classroom = classroomWith(null, teacherAt(aSchool()), Set.of());
        assertDoesNotThrow(() -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void allowsCreatingClassroomWhenTutorAndTeachersBelongToSameSchool() {
        final School school = aSchool();
        final Classroom classroom = classroomWith(school, teacherAt(school), Set.of(teacherAt(school)));
        assertDoesNotThrow(() -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void rejectsCreatingClassroomWhenTutorBelongsToAnotherSchool() {
        final Classroom classroom = classroomWith(aSchool(), teacherAt(aSchool()), Set.of());
        assertThrows(SchoolMismatchException.class, () -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void rejectsCreatingClassroomWhenATeacherBelongsToAnotherSchool() {
        final School school = aSchool();
        final Classroom classroom = classroomWith(school, null, Set.of(teacherAt(aSchool())));
        assertThrows(SchoolMismatchException.class, () -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void allowsLinkingWhenTutorAndTeachersBelongToSameSchool() {
        final School school = aSchool();
        final Classroom classroom = classroomWith(school, teacherAt(school), Set.of(teacherAt(school)));
        assertDoesNotThrow(() -> this.handler.verifyLinkedTeachersBelongToSchool(classroom, null));
    }

    @Test
    void rejectsLinkingATutorFromAnotherSchool() {
        final Classroom classroom = classroomWith(aSchool(), teacherAt(aSchool()), Set.of());
        assertThrows(SchoolMismatchException.class,
                     () -> this.handler.verifyLinkedTeachersBelongToSchool(classroom, null));
    }

    private static Classroom classroomWith(final School school, final Teacher tutor, final Set<Teacher> teachers) {
        //@formatter:off
        return Instancio.of(Classroom.class)
                        .set(field(Classroom.class, "school"), school)
                        .set(field(Classroom.class, "tutor"), tutor)
                        .set(field(Classroom.class, "teachers"), teachers)
                        .ignore(field(Classroom.class, "kids"))
                        .create();
        //@formatter:on
    }

    private static School aSchool() {
        //@formatter:off
        return Instancio.of(School.class)
                        .ignore(field(School.class, "classrooms"))
                        .ignore(field(School.class, "teachers"))
                        .create();
        //@formatter:off
    }

    private static Teacher teacherAt(final School school) {
        return Instancio.of(Teacher.class).set(field(Teacher.class, "school"), school).create();
    }

    @Test
    void rejectsLinkingATeacherFromAnotherSchool() {
        final School school = aSchool();
        final Classroom classroom = classroomWith(school, null, Set.of(teacherAt(aSchool())));
        assertThrows(SchoolMismatchException.class,
                     () -> this.handler.verifyLinkedTeachersBelongToSchool(classroom, null));
    }

}
