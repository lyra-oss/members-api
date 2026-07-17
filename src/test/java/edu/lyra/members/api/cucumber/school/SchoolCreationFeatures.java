package edu.lyra.members.api.cucumber.school;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SchoolCreationFeatures
        extends AbstractResourceFeatures {

    private final ObjectNode body = OBJECT_MAPPER.createObjectNode();

    @Before
    public void clearBody() {
        this.body.removeAll();
    }

    @Given("the school name is {string}")
    public void schoolNameIs(final String name) {
        this.body.put("name", name);
    }

    @Given("the school name is longer than 100 characters")
    public void schoolNameTooLong() {
        this.body.put("name", "a".repeat(101));
    }

    @Given("the school name is not provided")
    public void schoolNameNotProvided() {
        // name field intentionally omitted from the request
    }

    @Given("the school name is set to null")
    public void schoolNameIsNull() {
        this.body.putNull("name");
    }

    @Given("the school name is left blank")
    public void schoolNameIsBlank() {
        this.body.put("name", "");
    }

    @Given("the school name contains only whitespace")
    public void schoolNameContainsOnlyWhitespace() {
        this.body.put("name", "   ");
    }

    @When("I click on \"Create school\"")
    public void createSchool()
            throws Exception {
        this.performWithBody(post("/v0/schools"), this.body);
    }

    @Then("I receive a confirmation that the school has been successfully created")
    public void schoolCreatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

}
