package edu.lyra.members.api.config.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiBasePathTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void bindsTheConfiguredBasePath() {
        this.contextRunner.withPropertyValues("lyra.api.base-path=/v0")
                           .run(context -> assertEquals("/v0", context.getBean(ApiBasePath.class).basePath()));
    }

    @Test
    void rejectsABlankBasePath() {
        this.contextRunner.withPropertyValues("lyra.api.base-path=")
                           .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @EnableConfigurationProperties(ApiBasePath.class)
    static class TestConfig {}

}
