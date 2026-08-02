package edu.lyra.members.api.person.rest;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PersonRestConfiguration {

    @Bean
    PersonMapper personMapper() {
        return Mappers.getMapper(PersonMapper.class);
    }

    @Bean
    PersonAdapter personAdapter(
            final PersonRepository personRepository,
            final ParentRepository parentRepository,
            final TeacherRepository teacherRepository,
            final SchoolRepository schoolRepository,
            final ClassroomRepository classroomRepository,
            final PersonMapper mapper
    ) {
        return new PersonAdapter(personRepository, parentRepository, teacherRepository, schoolRepository,
                                 classroomRepository, mapper);
    }

}
