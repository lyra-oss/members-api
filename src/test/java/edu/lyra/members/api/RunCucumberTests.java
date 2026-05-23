package edu.lyra.members.api;

import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("edu/lyra/members/api")
class RunCucumberTests {

    @CucumberContextConfiguration
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    static class CucumberSpringConfiguration {

        @TestConfiguration
        static class SecurityConfig {

            @Bean
            MockMvc mockMvc(WebApplicationContext wac) {
                return MockMvcBuilders.webAppContextSetup(wac).apply(SecurityMockMvcConfigurers.springSecurity())
                                      .build();
            }

            @Bean
            JwtDecoder jwtDecoder() {
                return _ -> {
                    throw new JwtException("Test JwtDecoder — use jwt() post-processor instead");
                };
            }

        }

    }

}
