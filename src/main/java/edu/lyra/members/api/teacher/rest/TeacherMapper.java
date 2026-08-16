package edu.lyra.members.api.teacher.rest;

import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface TeacherMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "school", source = "resolvedSchool")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "surname", source = "request.surname")
    @Mapping(target = "mail", source = "request.mail")
    @BeanMapping(builder = @Builder(disableBuilder = true))
    Teacher toEntity(TeacherRequest request, School resolvedSchool);

    TeacherModel toModel(Teacher teacher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "school", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(TeacherPatchRequest request, @MappingTarget Teacher teacher);

}
