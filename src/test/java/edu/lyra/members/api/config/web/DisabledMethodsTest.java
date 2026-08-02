package edu.lyra.members.api.config.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.UUID.randomUUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pins the method/path combinations that RestExposureConfiguration used to close off at the Spring
// Data REST layer, so that guarantee survives its deletion (Phase 4.4) as an explicit assertion rather
// than silently widening the API. None of these methods are mapped by the new controllers at all, so
// Spring MVC itself rejects them with 405 once Security lets the request through.
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class DisabledMethodsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApiBasePath apiBasePath;

    private String base() {
        return this.apiBasePath.basePath();
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "teachers", "schools", "classrooms" })
    void itemPutIsDisabled(final String resource)
            throws Exception {
        //@formatter:off
        this.mvc.perform(put(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt())
                .contextPath(this.base())
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        //@formatter:on
    }

    @Test
    void creatingAPersonDirectlyIsDisabled()
            throws Exception {
        //@formatter:off
        this.mvc.perform(post(this.base() + "/persons")
                .with(jwt())
                .contextPath(this.base())
                .contentType(APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        //@formatter:on
    }

    @TestConfiguration
    static class Config {

        @Bean
        JwtDecoder jwtDecoder() {
            return _ -> {
                throw new JwtException("Test JwtDecoder — use jwt() post-processor instead");
            };
        }

    }

}
