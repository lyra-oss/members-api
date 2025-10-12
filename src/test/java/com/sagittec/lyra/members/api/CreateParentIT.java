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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("embedded")
@AutoConfigureMockMvc
class CreateParentIT {

    private static final ObjectMapper OM = new ObjectMapper();

    private static final String VALID_NAME    = "Esteban";
    private static final String VALID_SURNAME = "Cristóbal";
    private static final String VALID_EMAIL   = "esteban.cristobal@example.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createParent_withValidPayload_shouldReturnCreated()
            throws Exception {
        //@formatter:off
        final ObjectNode parentJson = this.objectMapper.createObjectNode()
                .put("name", VALID_NAME)
                .put("surname", VALID_SURNAME)
                .put("mail", VALID_EMAIL);
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

    private static List<Arguments> invalidParentPayloads() {
        //@formatter:off
        return List.of(
                // Size validations already covered
                arguments("Name too long", OM.createObjectNode()
                        .put("name", "a".repeat(101))
                        .put("surname", VALID_SURNAME)
                        .put("mail", VALID_EMAIL)),
                arguments("Surname too long", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", "b".repeat(101))
                        .put("mail", VALID_EMAIL)),
                arguments("E-mail too long", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .put("mail", "c".repeat(201) + "@example.com")),
                arguments("Invalid e-mail format", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .put("mail", "not-an-email")),
                arguments("Missing name (NotBlank)", OM.createObjectNode()
                        .put("surname", VALID_SURNAME)
                        .put("mail", VALID_EMAIL)),
                arguments("Null name (NotBlank)", OM.createObjectNode()
                        .putNull("name")
                        .put("surname", VALID_SURNAME)
                        .put("mail", VALID_EMAIL)),
                arguments("Empty name (NotBlank)", OM.createObjectNode()
                        .put("name", "")
                        .put("surname", VALID_SURNAME)
                        .put("mail", VALID_EMAIL)),
                arguments("Whitespace name (NotBlank)", OM.createObjectNode()
                        .put("name", "   ")
                        .put("surname", VALID_SURNAME)
                        .put("mail", VALID_EMAIL)),
                arguments("Missing surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("mail", VALID_EMAIL)),
                arguments("Null surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .putNull("surname")
                        .put("mail", VALID_EMAIL)),
                arguments("Empty surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", "")
                        .put("mail", VALID_EMAIL)),
                arguments("Whitespace surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", "   ")
                        .put("mail", VALID_EMAIL)),
                arguments("Missing e-mail (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)),
                arguments("Null e-mail (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .putNull("mail")),
                arguments("Empty e-mail (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .put("mail", "")),
                arguments("Whitespace e-mail (Email invalid)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .put("mail", "   "))
        );
        //@formatter:on
    }

}
