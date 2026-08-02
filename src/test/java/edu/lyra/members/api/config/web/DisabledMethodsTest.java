package edu.lyra.members.api.config.web;

import edu.lyra.members.api.config.security.TestJwtDecoderConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.UUID.randomUUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestJwtDecoderConfiguration.class)
class DisabledMethodsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApiBasePath apiBasePath;

    private String base() {
        return this.apiBasePath.basePath();
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
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

}
