package edu.lyra.members.api.cucumber.kid;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KidCreationFeatures
        extends AbstractResourceFeatures {

    private final ObjectNode body = OBJECT_MAPPER.createObjectNode();

    @Before
    public void clearBody() {
        this.body.removeAll();
    }

    @And("the kid name is {string}")
    public void kidNameIs(final String name) {
        this.body.put("name", name);
    }

    @And("the kid surname is {string}")
    public void kidSurnameIs(final String surname) {
        this.body.put("surname", surname);
    }

    @And("the kid birth date is {string}")
    public void kidBirthDateIs(final String birthdate) {
        this.body.put("birthdate", birthdate);
    }

    @When("I add the kid to my account")
    public void addKid()
            throws Exception {
        this.performWithBody(post("/v0/kids"), this.body);
    }

    @Then("I receive a confirmation that the kid has been successfully added")
    public void kidAddedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

}
