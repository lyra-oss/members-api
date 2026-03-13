package edu.lyra.members.api;

import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Suite
@IncludeEngines("cucumber")
class RunCucumberTests {

    @CucumberContextConfiguration
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    @ActiveProfiles("embedded")
    static class CucumberSpringConfiguration {

        @TestConfiguration
        static class MockMvcConfig {

            @Bean
            MockMvc mockMvc(WebApplicationContext wac) {
                return MockMvcBuilders.webAppContextSetup(wac).build();
            }

        }

    }

}
