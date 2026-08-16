package edu.lyra.members.api.classroom.rest;

import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClassroomMapperTest {

    private final ClassroomMapper mapper = Mappers.getMapper(ClassroomMapper.class);

    @Test
    void toEntityMapsCourseGroupSchoolAndTutorAndLeavesTheIdUnset() {
        final School  school = aSchool();
        final Teacher tutor  = aTeacher(school);
        final Classroom classroom =
                this.mapper.toEntity(new ClassroomRequest(3, "A", school.getId(), tutor.getId()), school, tutor);
        assertEquals(3, classroom.getCourse());
        assertEquals("A", classroom.getGroup());
        assertEquals(school, classroom.getSchool());
        assertEquals(tutor, classroom.getTutor());
        assertNull(classroom.getId());
    }

    private static School aSchool() {
        final School school = new School();
        school.setName("Gloria Fuertes");
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        return school;
    }

    private static Teacher aTeacher(final School school) {
        final Teacher teacher = new Teacher();
        teacher.setSchool(school);
        ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
        return teacher;
    }

    @Test
    void toEntityAllowsANullTutor() {
        final School school = aSchool();
        final Classroom classroom =
                this.mapper.toEntity(new ClassroomRequest(3, "A", school.getId(), null), school, null);
        assertNull(classroom.getTutor());
    }

    @Test
    void toModelMapsIdCourseAndGroup() {
        final Classroom classroom = new Classroom();
        classroom.setCourse(3);
        classroom.setGroup("A");
        ReflectionTestUtils.setField(classroom, "id", UUID.randomUUID());
        final ClassroomModel model = this.mapper.toModel(classroom);
        assertEquals(classroom.getId(), model.getId());
        assertEquals(3, model.getCourse());
        assertEquals("A", model.getGroup());
    }

    @Test
    void updateAppliesOnlyTheProvidedFields() {
        final Classroom classroom = new Classroom();
        classroom.setCourse(3);
        classroom.setGroup("A");
        this.mapper.update(new ClassroomPatchRequest(4, null), classroom);
        assertEquals(4, classroom.getCourse());
        assertEquals("A", classroom.getGroup());
    }

}
