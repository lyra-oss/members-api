package edu.lyra.members.api.kid.rest;

import java.util.List;

import edu.lyra.members.api.kid.KidRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KidRestConfiguration {

    @Bean
    KidsCollectionController kidsCollectionController(final KidVisibilityStrategyResolver visibilityResolver) {
        return new KidsCollectionController(visibilityResolver);
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
