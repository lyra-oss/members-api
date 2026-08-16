package edu.lyra.members.api.parent.rest;

import edu.lyra.members.api.parent.Parent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface ParentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @BeanMapping(builder = @Builder(disableBuilder = true))
    Parent toEntity(ParentRequest request);

    ParentModel toModel(Parent parent);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ParentPatchRequest request, @MappingTarget Parent parent);

}
