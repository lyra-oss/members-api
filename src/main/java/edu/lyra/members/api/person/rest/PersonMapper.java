package edu.lyra.members.api.person.rest;

import edu.lyra.members.api.person.Person;
import org.mapstruct.Mapper;

@Mapper
interface PersonMapper {

    PersonModel toModel(Person person);

}
