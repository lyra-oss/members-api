package edu.lyra.members.api.teacher.rest;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TeacherRestConfiguration {

    @Bean
    TeacherPolicy teacherPolicy(final ClassroomRepository classroomRepository) {
        return new TeacherPolicy(classroomRepository);
    }

    @Bean
    TeacherAdapter teacherAdapter(
            final TeacherRepository teacherRepository,
            final SchoolRepository schoolRepository,
            final PersonRepository personRepository,
            final ClassroomRepository classroomRepository,
            final TeacherMapper mapper,
            final TeacherPolicy policy
    ) {
        //@formatter:off
        return new TeacherAdapter(teacherRepository, schoolRepository, personRepository, classroomRepository, mapper,
                                  policy);
        //@formatter:on
    }

}
