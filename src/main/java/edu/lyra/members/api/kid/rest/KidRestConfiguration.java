package edu.lyra.members.api.kid.rest;

import java.util.List;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.ParentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KidRestConfiguration {

    @Bean
    KidPolicy kidPolicy(final ParentRepository parentRepository) {
        return new KidPolicy(parentRepository);
    }

    @Bean
    KidAdapter kidAdapter(
            final KidRepository kidRepository,
            final ParentRepository parentRepository,
            final ClassroomRepository classroomRepository,
            final KidVisibilityStrategyResolver visibilityResolver,
            final KidMapper mapper,
            final KidPolicy policy
    ) {
        return new KidAdapter(kidRepository, parentRepository, classroomRepository, visibilityResolver, mapper,
                              policy);
    }

    @Bean
    KidVisibilityStrategyResolver kidVisibilityStrategyResolver(
            final AdminKidVisibilityStrategy adminStrategy,
            final ParentKidVisibilityStrategy parentStrategy,
            final TeacherKidVisibilityStrategy teacherStrategy
    ) {
        return new KidVisibilityStrategyResolver(List.of(adminStrategy, parentStrategy, teacherStrategy));
    }

    @Bean
    AdminKidVisibilityStrategy adminKidVisibilityStrategy(final KidRepository kidRepository) {
        return new AdminKidVisibilityStrategy(kidRepository);
    }

    @Bean
    ParentKidVisibilityStrategy parentKidVisibilityStrategy(final KidRepository kidRepository) {
        return new ParentKidVisibilityStrategy(kidRepository);
    }

    @Bean
    TeacherKidVisibilityStrategy teacherKidVisibilityStrategy(final KidRepository kidRepository) {
        return new TeacherKidVisibilityStrategy(kidRepository);
    }

}
