package edu.lyra.members.api.person.rest;

import edu.lyra.members.api.person.Person;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PersonMapper {

    PersonModel toModel(Person person);

}
