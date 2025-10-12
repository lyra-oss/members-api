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

import static java.time.LocalDate.now;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("embedded")
@AutoConfigureMockMvc
class CreateKidIT {

    private static final ObjectMapper OM = new ObjectMapper();

    private static final String VALID_NAME      = "Alicia";
    private static final String VALID_SURNAME   = "Cristóbal";
    private static final String VALID_BIRTHDATE = now().minusYears(5).toString();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createKid_withValidPayload_shouldReturnCreated()
            throws Exception {
        // @formatter:off
        final ObjectNode kidJson = this.objectMapper.createObjectNode()
                .put("name", VALID_NAME)
                .put("surname", VALID_SURNAME)
                .put("birthdate", VALID_BIRTHDATE);
        this.mvc.perform(post("/kids")
                        .contentType(APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(kidJson)))
                .andExpect(status().isCreated());
        // @formatter:on
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidKidPayloads")
    void createKid_withInvalidPayload_shouldReturnBadRequest(String ignoredCaseName, ObjectNode kidJson)
            throws Exception {
        // @formatter:off
        this.mvc.perform(post("/kids")
                        .contentType(APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(kidJson)))
                .andExpect(status().isBadRequest());
        // @formatter:on
    }

    private static List<Arguments> invalidKidPayloads() {
        // @formatter:off
        return List.of(
                arguments("Name too long", OM.createObjectNode()
                        .put("name", "a".repeat(101))
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Surname too long", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", "b".repeat(101))
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Missing name (NotBlank)", OM.createObjectNode()
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Null name (NotBlank)", OM.createObjectNode()
                        .putNull("name")
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Empty name (NotBlank)", OM.createObjectNode()
                        .put("name", "")
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Whitespace name (NotBlank)", OM.createObjectNode()
                        .put("name", "   ")
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Missing surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Null surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .putNull("surname")
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Empty surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", "")
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Whitespace surname (NotBlank)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", "   ")
                        .put("birthdate", VALID_BIRTHDATE)),
                arguments("Missing birthdate (NotNull)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)),
                arguments("Null birthdate (NotNull)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .putNull("birthdate")),
                arguments("Empty birthdate (parse error)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", "")),
                arguments("Whitespace birthdate (parse error)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", "   ")),
                arguments("Future birthdate (Past)", OM.createObjectNode()
                        .put("name", VALID_NAME)
                        .put("surname", VALID_SURNAME)
                        .put("birthdate", now().plusDays(1).toString()))
        );
        // @formatter:on
    }

}
