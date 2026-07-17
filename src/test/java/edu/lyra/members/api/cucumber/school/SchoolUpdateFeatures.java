package edu.lyra.members.api.cucumber.school;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SchoolUpdateFeatures
        extends AbstractResourceFeatures {

    @When("I update school {string}'s name to {string}")
    public void updateSchoolName(final String schoolName, final String newName)
            throws Exception {
        this.performUpdateName(this.scenarioContext.getLocation("school:" + schoolName), newName);
    }

    private void performUpdateName(final String location, final String newName)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("name", newName);
        this.performWithBody(patch(location), body);
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
