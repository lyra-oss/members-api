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

public class SchoolUpdateFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("I update school {string}'s name to {string}")
    public void updateSchoolName(final String schoolName, final String newName)
            throws Exception {
        this.performUpdateName(this.scenarioContext.getLocation("school:" + schoolName), newName);
    }

    private void performUpdateName(final String location, final String newName)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("name", newName);
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                patch(location).with(this.scenarioContext.getJwtProcessor())
                               .contentType(APPLICATION_JSON)
                               .content(OBJECT_MAPPER.writeValueAsString(body))));
        //@formatter:on
    }

    @When("I update a school that does not exist")
    public void updateNonExistentSchool()
            throws Exception {
        this.performUpdateName("/v0/schools/" + UUID.randomUUID(), "Doesn't matter");
    }

    @Then("I receive a confirmation that the school has been successfully updated")
    public void schoolUpdatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
