package edu.lyra.members.api.cucumber.parent;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ParentDeleteFeatures
        extends AbstractResourceFeatures {

    @When("I delete parent {string} {string}")
    public void deleteParent(final String name, final String surname)
            throws Exception {
        this.perform(delete(this.scenarioContext.getLocation("parent:" + name + " " + surname)));
    }

    @When("I delete a parent that does not exist")
    public void deleteNonExistentParent()
            throws Exception {
        this.perform(delete("/v0/parents/" + UUID.randomUUID()));
    }

    @Then("I receive a confirmation that the parent account has been successfully deleted")
    public void parentDeletedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
