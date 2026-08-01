package edu.lyra.members.api.person.rest;

import java.util.UUID;

import edu.lyra.members.api.person.Person;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonMapperTest {

    private final PersonMapper mapper = Mappers.getMapper(PersonMapper.class);

    @Test
    void toModelMapsIdNameSurnameAndMail() {
        //@formatter:off
        final Person person = Person.builder().id(UUID.randomUUID())
                                    .name("Esteban")
                                    .surname("Cristóbal")
                                    .mail("esteban.cristobal@example.com")
                                    .build();
        //@formatter:on
        final PersonModel model = this.mapper.toModel(person);
        assertEquals(person.getId(), model.getId());
        assertEquals("Esteban", model.getName());
        assertEquals("Cristóbal", model.getSurname());
        assertEquals("esteban.cristobal@example.com", model.getMail());
    }

}
