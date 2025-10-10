package com.sagittec.lyra.members.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

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
    void createParent_shouldReturnCreated()
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

    @Test
    void createParentWithNameTooLong_shouldReturnBadRequest()
            throws Exception {
        //@formatter:off
        final String longName = "a".repeat(101); // max = 100
        final ObjectNode parentJson = this.objectMapper.createObjectNode()
                .put("name", longName)
                .put("surname", "Cristóbal")
                .put("mail", "esteban.cristobal@example.com");

        this.mvc.perform(post("/parents")
                                 .contentType(APPLICATION_JSON)
                                 .content(this.objectMapper.writeValueAsBytes(parentJson)))
                .andExpect(status().isBadRequest());
        //@formatter:on
    }

    @Test
    void createParentWithSurnameTooLong_shouldReturnBadRequest()
            throws Exception {
        //@formatter:off
        final String longSurname = "b".repeat(101); // max = 100
        final ObjectNode parentJson = this.objectMapper.createObjectNode()
                .put("name", "Esteban")
                .put("surname", longSurname)
                .put("mail", "esteban.cristobal@example.com");
        this.mvc.perform(post("/parents")
                                 .contentType(APPLICATION_JSON)
                                 .content(this.objectMapper.writeValueAsBytes(parentJson)))
                .andExpect(status().isBadRequest());
        //@formatter:on
    }

    @Test
    void createParentWithMailTooLong_shouldReturnBadRequest()
            throws Exception {
        //@formatter:off
        final String longLocal = "c".repeat(201) + "@example.com"; // email length > 200 in entity
        final ObjectNode parentJson = this.objectMapper.createObjectNode()
                .put("name", "Esteban")
                .put("surname", "Cristóbal")
                .put("mail", longLocal);
        this.mvc.perform(post("/parents")
                                 .contentType(APPLICATION_JSON)
                                 .content(this.objectMapper.writeValueAsBytes(parentJson)))
                .andExpect(status().isBadRequest());
        //@formatter:on
    }

    @Test
    void createParentWithInvalidEmail_shouldReturnBadRequest()
            throws Exception {
        //@formatter:off
        final ObjectNode parentJson = this.objectMapper.createObjectNode()
                .put("name", "Esteban")
                .put("surname", "Cristóbal")
                .put("mail", "not-an-email");
        this.mvc.perform(post("/parents")
                                 .contentType(APPLICATION_JSON)
                                 .content(this.objectMapper.writeValueAsBytes(parentJson)))
                .andExpect(status().isBadRequest());
        //@formatter:on
    }

}
