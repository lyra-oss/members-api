package edu.lyra.members.api.cucumber.school;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SchoolDeleteFeatures
        extends AbstractResourceFeatures {

    @When("I delete school {string}")
    public void deleteSchool(final String schoolName)
            throws Exception {
        this.perform(delete(this.scenarioContext.getLocation("school:" + schoolName)));
    }

    @When("I delete a school that does not exist")
    public void deleteNonExistentSchool()
            throws Exception {
        this.perform(delete("/v0/schools/" + UUID.randomUUID()));
    }

    @Then("I receive a confirmation that the school has been successfully deleted")
    public void schoolDeletedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
