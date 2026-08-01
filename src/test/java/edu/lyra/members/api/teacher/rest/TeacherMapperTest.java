package edu.lyra.members.api.teacher.rest;

import java.util.UUID;

import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TeacherMapperTest {

    private final TeacherMapper mapper = Mappers.getMapper(TeacherMapper.class);

    private static School aSchool(final String name) {
        final School school = new School();
        school.setName(name);
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        return school;
    }

    @Test
    void toEntityMapsNameSurnameMailAndSchoolAndLeavesTheIdUnset() {
        final School  school  = aSchool("Gloria Fuertes");
        final Teacher teacher = this.mapper.toEntity(
                new TeacherRequest("Marta", "Ibáñez", "marta.ibanez@example.com", school.getId()), school);
        assertEquals("Marta", teacher.getName());
        assertEquals("Ibáñez", teacher.getSurname());
        assertEquals("marta.ibanez@example.com", teacher.getMail());
        assertEquals(school, teacher.getSchool());
        assertNull(teacher.getId());
    }

    @Test
    void toModelMapsIdNameSurnameAndMail() {
        final Teacher teacher = new Teacher();
        teacher.setName("Marta");
        teacher.setSurname("Ibáñez");
        teacher.setMail("marta.ibanez@example.com");
        ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
        final TeacherModel model = this.mapper.toModel(teacher);
        assertEquals(teacher.getId(), model.getId());
        assertEquals("Marta", model.getName());
        assertEquals("Ibáñez", model.getSurname());
        assertEquals("marta.ibanez@example.com", model.getMail());
    }

    @Test
    void updateAppliesOnlyTheProvidedFields() {
        final Teacher teacher = new Teacher();
        teacher.setName("Marta");
        teacher.setSurname("Ibáñez");
        teacher.setMail("marta.ibanez@example.com");
        this.mapper.update(new TeacherPatchRequest(null, "Ibáñez Ruiz", null), teacher);
        assertEquals("Marta", teacher.getName());
        assertEquals("Ibáñez Ruiz", teacher.getSurname());
        assertEquals("marta.ibanez@example.com", teacher.getMail());
    }

}
