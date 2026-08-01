package edu.lyra.members.api.parent.rest;

import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParentMapperTest {

    private final ParentMapper mapper = Mappers.getMapper(ParentMapper.class);

    @Test
    void toEntityMapsNameSurnameAndMailAndLeavesTheIdUnset() {
        final Parent parent =
                this.mapper.toEntity(new ParentRequest("Esteban", "Cristóbal", "esteban.cristobal@example.com"));
        assertEquals("Esteban", parent.getName());
        assertEquals("Cristóbal", parent.getSurname());
        assertEquals("esteban.cristobal@example.com", parent.getMail());
        assertNull(parent.getId());
    }

    @Test
    void toModelMapsIdNameSurnameAndMail() {
        final Parent parent = new Parent();
        parent.setName("Esteban");
        parent.setSurname("Cristóbal");
        parent.setMail("esteban.cristobal@example.com");
        ReflectionTestUtils.setField(parent, "id", UUID.randomUUID());
        final ParentModel model = this.mapper.toModel(parent);
        assertEquals(parent.getId(), model.getId());
        assertEquals("Esteban", model.getName());
        assertEquals("Cristóbal", model.getSurname());
        assertEquals("esteban.cristobal@example.com", model.getMail());
    }

    @Test
    void updateAppliesOnlyTheProvidedFields() {
        final Parent parent = new Parent();
        parent.setName("Esteban");
        parent.setSurname("Cristóbal");
        parent.setMail("esteban.cristobal@example.com");
        this.mapper.update(new ParentPatchRequest(null, "Cristóbal Ruiz", null), parent);
        assertEquals("Esteban", parent.getName());
        assertEquals("Cristóbal Ruiz", parent.getSurname());
        assertEquals("esteban.cristobal@example.com", parent.getMail());
    }

}
