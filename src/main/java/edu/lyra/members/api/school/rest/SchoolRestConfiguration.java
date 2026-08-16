package edu.lyra.members.api.school.rest;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SchoolRestConfiguration {

    @Bean
    SchoolPolicy schoolPolicy() {
        return new SchoolPolicy();
    }

    @Bean
    SchoolAdapter schoolAdapter(
            final SchoolRepository repository,
            final TeacherRepository teacherRepository,
            final ClassroomRepository classroomRepository,
            final SchoolMapper mapper,
            final SchoolPolicy policy
    ) {
        return new SchoolAdapter(repository, teacherRepository, classroomRepository, mapper, policy);
    }

}
