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

public class KidUpdateFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("I update kid {string} {string}'s surname to {string}")
    public void updateKidSurname(final String name, final String surname, final String newSurname)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("surname", newSurname);
        this.performPatch(this.scenarioContext.getLocation("kid:" + name + " " + surname), body);
    }

    private void performPatch(final String location, final ObjectNode body)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                patch(location).with(this.scenarioContext.getJwtProcessor())
                               .contentType(APPLICATION_JSON)
                               .content(OBJECT_MAPPER.writeValueAsString(body))));
        //@formatter:on
    }

    @When("I update kid {string} {string}'s parent to {string}")
    public void updateKidParent(final String name, final String surname, final String parentName)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("parent", this.scenarioContext.getLocation("parent:" + parentName));
        this.performPatch(this.scenarioContext.getLocation("kid:" + name + " " + surname), body);
    }

    @When("I update kid {string} {string}'s classroom to course {int} group {string}")
    public void updateKidClassroom(final String name, final String surname, final int course, final String group)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("classroom", this.scenarioContext.getLocation("classroom:" + course + " " + group));
        this.performPatch(this.scenarioContext.getLocation("kid:" + name + " " + surname), body);
    }

    @When("I update a kid that does not exist")
    public void updateNonExistentKid()
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("surname", "Doesn't matter");
        this.performPatch("/v0/kids/" + UUID.randomUUID(), body);
    }

    @Then("I receive a confirmation that the kid has been successfully updated")
    public void kidUpdatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
