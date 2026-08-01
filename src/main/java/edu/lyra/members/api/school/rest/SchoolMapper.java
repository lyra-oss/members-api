package edu.lyra.members.api.school.rest;

import edu.lyra.members.api.school.School;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
interface SchoolMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classrooms", ignore = true)
    @Mapping(target = "teachers", ignore = true)
    School toEntity(SchoolRequest request);

    SchoolModel toModel(School school);

    // null means "not supplied" - absent request fields leave the existing entity value untouched.
    @Mapping(target = "classrooms", ignore = true)
    @Mapping(target = "teachers", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(SchoolRequest request, @MappingTarget School school);

}
