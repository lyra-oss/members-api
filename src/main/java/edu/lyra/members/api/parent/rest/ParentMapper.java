package edu.lyra.members.api.parent.rest;

import edu.lyra.members.api.parent.Parent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
interface ParentMapper {

    // Parent's Lombok @Builder only exposes id/person (its private all-args constructor's
    // parameters) - name/surname/mail are PersonRole's delegating setters, which the builder cannot
    // reach. Disabling builder detection makes MapStruct fall back to new Parent() + setters, so
    // those three actually get set (and, as a side effect of PersonRole.setName/setSurname/setMail,
    // lazily populate the Person they delegate to).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @BeanMapping(builder = @Builder(disableBuilder = true))
    Parent toEntity(ParentRequest request);

    ParentModel toModel(Parent parent);

    // null means "not supplied" - absent request fields leave the existing entity value untouched.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ParentPatchRequest request, @MappingTarget Parent parent);

}
