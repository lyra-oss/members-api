package edu.lyra.members.api.cucumber.kid;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KidDeleteFeatures
        extends AbstractResourceFeatures {

    @When("I delete kid {string} {string}")
    public void deleteKid(final String name, final String surname)
            throws Exception {
        this.perform(delete(this.scenarioContext.getLocation("kid:" + name + " " + surname)));
    }

    @When("I delete a kid that does not exist")
    public void deleteNonExistentKid()
            throws Exception {
        this.perform(delete("/v0/kids/" + UUID.randomUUID()));
    }

    @Then("I receive a confirmation that the kid has been successfully deleted")
    public void kidDeletedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
