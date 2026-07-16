package edu.lyra.members.api;

import java.util.UUID;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TeacherUpdateFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("I update teacher {string} {string}'s surname to {string}")
    public void updateTeacherSurname(final String name, final String surname, final String newSurname)
            throws Exception {
        this.performUpdateSurname(this.scenarioContext.getLocation("teacher:" + name + " " + surname), newSurname);
    }

    private void performUpdateSurname(final String location, final String newSurname)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("surname", newSurname);
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                patch(location).with(this.scenarioContext.getJwtProcessor())
                               .contentType(APPLICATION_JSON)
                               .content(OBJECT_MAPPER.writeValueAsString(body))));
        //@formatter:on
    }

    @When("I update a teacher that does not exist")
    public void updateNonExistentTeacher()
            throws Exception {
        this.performUpdateSurname("/v0/teachers/" + UUID.randomUUID(), "Doesn't matter");
    }

    @Then("I receive a confirmation that the teacher account has been successfully updated")
    public void teacherAccountUpdatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
