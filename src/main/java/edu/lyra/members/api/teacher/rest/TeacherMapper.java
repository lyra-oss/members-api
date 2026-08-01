package edu.lyra.members.api.teacher.rest;

import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
interface TeacherMapper {

    // Teacher's Lombok @Builder only exposes id/person/school (its private all-args constructor's
    // parameters) - name/surname/mail are PersonRole's delegating setters, which the builder cannot
    // reach. Disabling builder detection makes MapStruct fall back to new Teacher() + setters, so
    // those three actually get set (and, as a side effect of PersonRole.setName/setSurname/setMail,
    // lazily populate the Person they delegate to).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "school", source = "resolvedSchool")
    // School also has a "name" property, so with two source parameters every request-sourced target
    // property needs its source parameter spelled out explicitly to stay unambiguous.
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "surname", source = "request.surname")
    @Mapping(target = "mail", source = "request.mail")
    @BeanMapping(builder = @Builder(disableBuilder = true))
    Teacher toEntity(TeacherRequest request, School resolvedSchool);

    TeacherModel toModel(Teacher teacher);

    // null means "not supplied" - absent request fields leave the existing entity value untouched.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "school", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(TeacherPatchRequest request, @MappingTarget Teacher teacher);

}
