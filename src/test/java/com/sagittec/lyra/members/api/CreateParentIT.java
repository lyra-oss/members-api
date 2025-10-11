package com.sagittec.lyra.members.api;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CreateParentIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createParent_withValidPayload_shouldReturnCreated()
            throws Exception {
        //@formatter:off
        final ObjectNode parentJson = this.objectMapper.createObjectNode()
                .put("name", "Esteban")
                .put("surname", "Cristóbal")
                .put("mail", "esteban.cristobal@example.com");
        this.mvc.perform(post("/parents")
                                 .contentType(APPLICATION_JSON)
                                 .content(this.objectMapper.writeValueAsBytes(parentJson)))
                .andExpect(status().isCreated());
        //@formatter:on
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidParentPayloads")
    void createParent_withInvalidPayload_shouldReturnBadRequest(String ignoredCaseName, ObjectNode parentJson)
            throws Exception {
        //@formatter:off
        this.mvc.perform(post("/parents")
                                 .contentType(APPLICATION_JSON)
                                 .content(this.objectMapper.writeValueAsBytes(parentJson)))
                .andExpect(status().isBadRequest());
        //@formatter:on
    }

    static List<Arguments> invalidParentPayloads() {
        //@formatter:off
        final ObjectMapper om = new ObjectMapper();
        final String longName = "a".repeat(101); // max = 100
        final String longSurname = "b".repeat(101); // max = 100
        final String longLocal = "c".repeat(201) + "@example.com"; // email length > 200 in entity
        return List.of(
                arguments("Name too long", om.createObjectNode()
                        .put("name", longName)
                        .put("surname", "Cristóbal")
                        .put("mail", "esteban.cristobal@example.com")),
                arguments("Surname too long", om.createObjectNode()
                        .put("name", "Esteban")
                        .put("surname", longSurname)
                        .put("mail", "esteban.cristobal@example.com")),
                arguments("E-mail too long", om.createObjectNode()
                        .put("name", "Esteban")
                        .put("surname", "Cristóbal")
                        .put("mail", longLocal)),
                arguments("Invalid e-mail", om.createObjectNode()
                        .put("name", "Esteban")
                        .put("surname", "Cristóbal")
                        .put("mail", "not-an-email"))
        );
        //@formatter:on
    }

}
