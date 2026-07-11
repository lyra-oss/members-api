package edu.lyra.members.api.handlers;

import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.Teacher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassroomTeacherAssignmentEventHandlerTest {

    private final ClassroomTeacherAssignmentEventHandler handler = new ClassroomTeacherAssignmentEventHandler();

    private static School schoolWithId() {
        final School school = new School();
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        return school;
    }

    private static Teacher teacherAt(final School school) {
        final Teacher teacher = new Teacher();
        ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(teacher, "school", school);
        return teacher;
    }

    private static Classroom classroomAt(final School school) {
        final Classroom classroom = new Classroom();
        ReflectionTestUtils.setField(classroom, "school", school);
        return classroom;
    }

    @Test
    void allowsCreatingClassroomWithoutTutorOrTeachers() {
        final Classroom classroom = classroomAt(schoolWithId());
        assertDoesNotThrow(() -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void allowsCreatingClassroomWhenSchoolIsNotYetSet() {
        final Classroom classroom = new Classroom();
        ReflectionTestUtils.setField(classroom, "tutor", teacherAt(schoolWithId()));
        assertDoesNotThrow(() -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void allowsCreatingClassroomWhenTutorAndTeachersBelongToSameSchool() {
        final School school = schoolWithId();
        final Classroom classroom = classroomAt(school);
        ReflectionTestUtils.setField(classroom, "tutor", teacherAt(school));
        ReflectionTestUtils.setField(classroom, "teachers", Set.of(teacherAt(school)));
        assertDoesNotThrow(() -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void rejectsCreatingClassroomWhenTutorBelongsToAnotherSchool() {
        final Classroom classroom = classroomAt(schoolWithId());
        ReflectionTestUtils.setField(classroom, "tutor", teacherAt(schoolWithId()));
        assertThrows(SchoolMismatchException.class, () -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

    @Test
    void rejectsCreatingClassroomWhenATeacherBelongsToAnotherSchool() {
        final School school = schoolWithId();
        final Classroom classroom = classroomAt(school);
        ReflectionTestUtils.setField(classroom, "teachers", Set.of(teacherAt(schoolWithId())));
        assertThrows(SchoolMismatchException.class, () -> this.handler.verifyTeachersBelongToSchool(classroom));
    }

}
