package edu.lyra.members.api.classroom.rest;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClassroomRestConfiguration {

    @Bean
    ClassroomMapper classroomMapper() {
        return Mappers.getMapper(ClassroomMapper.class);
    }

    @Bean
    ClassroomPolicy classroomPolicy() {
        return new ClassroomPolicy();
    }

    @Bean
    ClassroomAdapter classroomAdapter(
            final ClassroomRepository classroomRepository,
            final SchoolRepository schoolRepository,
            final TeacherRepository teacherRepository,
            final KidRepository kidRepository,
            final ClassroomMapper mapper,
            final ClassroomPolicy policy,
            final ApiBasePath apiBasePath
    ) {
        return new ClassroomAdapter(classroomRepository, schoolRepository, teacherRepository, kidRepository, mapper,
                                    policy, apiBasePath);
    }

}
