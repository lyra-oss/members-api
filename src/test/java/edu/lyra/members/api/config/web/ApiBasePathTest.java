package edu.lyra.members.api.config.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiBasePathTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void bindsTheConfiguredBasePath() {
        this.contextRunner.withPropertyValues("server.servlet.context-path=/v0")
                           .run(context -> assertEquals("/v0", context.getBean(ApiBasePath.class).contextPath()));
    }

    @Test
    void rejectsABlankBasePath() {
        this.contextRunner.withPropertyValues("server.servlet.context-path=")
                           .run(context -> {
                               final Throwable failure = context.getStartupFailure();
                               assertInstanceOf(ConfigurationPropertiesBindException.class, failure);
                               assertTrue(failure.getMessage().contains("ApiBasePath"));
                           });
    }

    @EnableConfigurationProperties(ApiBasePath.class)
    private static class TestConfig {}

}
