package edu.lyra.members.api.school.rest;

import java.util.UUID;

import edu.lyra.members.api.school.School;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SchoolMapperTest {

    private final SchoolMapper mapper = Mappers.getMapper(SchoolMapper.class);

    @Test
    void toEntityMapsTheNameAndLeavesTheIdUnset() {
        final School school = this.mapper.toEntity(new SchoolRequest("Gloria Fuertes"));
        assertEquals("Gloria Fuertes", school.getName());
        assertNull(school.getId());
    }

    @Test
    void toModelMapsIdAndName() {
        final School school = new School();
        school.setName("Gloria Fuertes");
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        final SchoolModel model = this.mapper.toModel(school);
        assertEquals(school.getId(), model.getId());
        assertEquals("Gloria Fuertes", model.getName());
    }

    @Test
    void updateAppliesTheProvidedNameOntoTheExistingEntity() {
        final School school = new School();
        school.setName("Old name");
        this.mapper.update(new SchoolRequest("New name"), school);
        assertEquals("New name", school.getName());
    }

}
