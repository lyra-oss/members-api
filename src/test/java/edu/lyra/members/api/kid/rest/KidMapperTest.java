package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KidMapperTest {

    private final KidMapper mapper = Mappers.getMapper(KidMapper.class);

    private static Parent aParent() {
        final Parent parent = new Parent();
        ReflectionTestUtils.setField(parent, "id", UUID.randomUUID());
        return parent;
    }

    private static Classroom aClassroom() {
        final Classroom classroom = new Classroom();
        ReflectionTestUtils.setField(classroom, "id", UUID.randomUUID());
        return classroom;
    }

    @Test
    void toEntityMapsNameSurnameAndBirthdateAndLeavesTheIdParentAndClassroomUnset() {
        final Kid kid = this.mapper.toEntity(new KidRequest("Alicia", "Cristóbal", LocalDate.of(2019, 12, 12)));
        assertEquals("Alicia", kid.getName());
        assertEquals("Cristóbal", kid.getSurname());
        assertEquals(LocalDate.of(2019, 12, 12), kid.getBirthdate());
        assertNull(kid.getId());
        assertNull(kid.getParent());
        assertNull(kid.getClassroom());
    }

    @Test
    void toModelMapsIdNameSurnameAndBirthdate() {
        final Kid kid = new Kid();
        kid.setName("Alicia");
        kid.setSurname("Cristóbal");
        kid.setBirthdate(LocalDate.of(2019, 12, 12));
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        final KidModel model = this.mapper.toModel(kid);
        assertEquals(kid.getId(), model.getId());
        assertEquals("Alicia", model.getName());
        assertEquals("Cristóbal", model.getSurname());
        assertEquals(LocalDate.of(2019, 12, 12), model.getBirthdate());
    }

    @Test
    void updateAppliesOnlyTheProvidedFields() {
        final Kid    kid    = new Kid();
        final Parent parent = aParent();
        kid.setName("Alicia");
        kid.setSurname("Cristóbal");
        kid.setBirthdate(LocalDate.of(2019, 12, 12));
        kid.setParent(parent);
        this.mapper.update(new KidPatchRequest(null, "Cristóbal Ruiz", null, null, null), kid, parent, null);
        assertEquals("Alicia", kid.getName());
        assertEquals("Cristóbal Ruiz", kid.getSurname());
        assertEquals(LocalDate.of(2019, 12, 12), kid.getBirthdate());
    }

    @Test
    void updateAppliesTheResolvedParentAndClassroomRegardlessOfNullValueStrategy() {
        final Kid       kid       = new Kid();
        final Parent    newParent = aParent();
        final Classroom classroom = aClassroom();
        this.mapper.update(new KidPatchRequest(null, null, null, null, null), kid, newParent, classroom);
        assertEquals(newParent, kid.getParent());
        assertEquals(classroom, kid.getClassroom());
    }

}
