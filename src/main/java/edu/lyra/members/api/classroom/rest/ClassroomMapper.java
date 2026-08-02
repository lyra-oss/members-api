package edu.lyra.members.api.classroom.rest;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
interface ClassroomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teachers", ignore = true)
    @Mapping(target = "kids", ignore = true)
    @Mapping(target = "school", source = "resolvedSchool")
    @Mapping(target = "tutor", source = "resolvedTutor")
    Classroom toEntity(ClassroomRequest request, School resolvedSchool, Teacher resolvedTutor);

    ClassroomModel toModel(Classroom classroom);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "school", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "teachers", ignore = true)
    @Mapping(target = "kids", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ClassroomPatchRequest request, @MappingTarget Classroom classroom);

}
