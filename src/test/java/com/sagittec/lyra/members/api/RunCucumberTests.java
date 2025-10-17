package com.sagittec.lyra.members.api;

import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Suite
@IncludeEngines("cucumber")
class RunCucumberTests {

    @CucumberContextConfiguration
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("embedded")
    static class CucumberSpringConfiguration {}

}
