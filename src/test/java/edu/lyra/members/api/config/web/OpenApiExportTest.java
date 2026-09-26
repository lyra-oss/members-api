package edu.lyra.members.api.config.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import edu.lyra.members.api.config.security.TestJwtDecoderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Writes the application's OpenAPI description to {@code target/openapi.json} - the single source of truth the
 * committed Postman collection is generated from (see {@code scripts/generate-postman-collection.sh}), so the API
 * is never defined twice. springdoc's {@code /v3/api-docs} endpoint stays disabled everywhere else (see
 * {@code application.properties}); this test is the one place that turns it on, in its own isolated context, and
 * only reaches it in-process through {@link MockMvc} - never over a real network listener.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "springdoc.api-docs.enabled=true")
@Import(TestJwtDecoderConfiguration.class)
class OpenApiExportTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApiBasePath apiBasePath;

    @Test
    void exportsTheOpenApiDescription()
            throws Exception {
        final String contextPath = this.apiBasePath.contextPath();
        //@formatter:off
        final String body = this.mvc.perform(get(contextPath + "/v3/api-docs").contextPath(contextPath).with(jwt()))
                                     .andExpect(status().isOk())
                                     .andReturn().getResponse().getContentAsString();
        //@formatter:on
        this.write(body);
    }

    private void write(final String openApiJson)
            throws IOException {
        final Path target = Path.of("target", "openapi.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, openApiJson);
    }

}
