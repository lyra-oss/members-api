package edu.lyra.members.api.kid.rest;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
interface KidMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "classroom", ignore = true)
    Kid toEntity(KidRequest request);

    KidModel toModel(Kid kid);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "surname", source = "request.surname")
    @Mapping(target = "birthdate", source = "request.birthdate")
    @Mapping(target = "parent", source = "newParent")
    @Mapping(target = "classroom", source = "newClassroom")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(KidPatchRequest request, @MappingTarget Kid kid, Parent newParent, Classroom newClassroom);

}
