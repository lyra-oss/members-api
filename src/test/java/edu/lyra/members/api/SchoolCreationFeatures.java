package edu.lyra.members.api;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SchoolCreationFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectNode school = OBJECT_MAPPER.createObjectNode();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScenarioContext scenarioContext;

    @Before
    public void cleanJson() {
        this.school.removeAll();
    }

    @Given("the school name is {string}")
    public void schoolNameIs(final String name) {
        this.school.put("name", name);
    }

    @Given("the school name is longer than 100 characters")
    public void schoolNameTooLong() {
        this.school.put("name", "a".repeat(101));
    }

    @Given("the school name is not provided")
    public void schoolNameNotProvided() {
        // name field intentionally omitted from the request
    }

    @Given("the school name is set to null")
    public void schoolNameIsNull() {
        this.school.putNull("name");
    }

    @Given("the school name is left blank")
    public void schoolNameIsBlank() {
        this.school.put("name", "");
    }

    @Given("the school name contains only whitespace")
    public void schoolNameContainsOnlyWhitespace() {
        this.school.put("name", "   ");
    }

    @When("I click on \"Create school\"")
    public void createSchool()
            throws Exception {
        final String content = OBJECT_MAPPER.writeValueAsString(this.school);
        this.scenarioContext.setResultActions(
                this.mvc.perform(post("/v0/schools").contentType(APPLICATION_JSON).content(content)));
    }

    @Then("I receive a confirmation that the school has been successfully created")
    public void schoolCreatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

}
