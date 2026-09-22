package edu.lyra.members.api.school.rest;

import edu.lyra.members.api.school.School;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface SchoolMapper {

    @Mapping(target = "id", ignore = true)
    School toEntity(SchoolRequest request);

    SchoolModel toModel(School school);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(SchoolRequest request, @MappingTarget School school);

}
