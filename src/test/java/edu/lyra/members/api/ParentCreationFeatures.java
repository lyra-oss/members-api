package edu.lyra.members.api;

import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
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

public class ParentCreationFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectNode parentJson = OBJECT_MAPPER.createObjectNode();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ParentsRepository parentsRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    @Before
    public void cleanJson() {
        this.parentJson.removeAll();
        this.parentsRepository.deleteAll();
    }

    @Given("my name is {string}")
    public void myNameIs(final String name) {
        this.parentJson.put("name", name);
    }

    @Given("my name is longer than 100 characters")
    public void myNameTooLong() {
        this.parentJson.put("name", "a".repeat(101));
    }

    @Given("my name is not provided")
    public void myNameNotProvided() {
        // name field intentionally omitted from the request
    }

    @Given("my name is set to null")
    public void myNameIsNull() {
        this.parentJson.putNull("name");
    }

    @Given("my name is left blank")
    public void myNameIsBlank() {
        this.parentJson.put("name", "");
    }

    @Given("my name contains only whitespace")
    public void myNameContainsOnlyWhitespace() {
        this.parentJson.put("name", "   ");
    }

    @And("my surname is {string}")
    public void mySurnameIs(final String surname) {
        this.parentJson.put("surname", surname);
    }

    @And("my surname is longer than 100 characters")
    public void mySurnameTooLong() {
        this.parentJson.put("surname", "b".repeat(101));
    }

    @And("my surname is not provided")
    public void mySurnameNotProvided() {
        // surname field intentionally omitted from the request
    }

    @And("my surname is set to null")
    public void mySurnameIsNull() {
        this.parentJson.putNull("surname");
    }

    @And("my surname is left blank")
    public void mySurnameIsBlank() {
        this.parentJson.put("surname", "");
    }

    @And("my surname contains only whitespace")
    public void mySurnameContainsOnlyWhitespace() {
        this.parentJson.put("surname", "   ");
    }

    @And("my e-mail address is {string}")
    public void myEMailAddressIs(final String mail) {
        this.parentJson.put("mail", mail);
    }

    @And("my e-mail address is longer than 200 characters")
    public void myEMailAddressTooLong() {
        this.parentJson.put("mail", "c".repeat(201) + "@example.com");
    }

    @And("my e-mail address is not provided")
    public void myEMailAddressNotProvided() {
        // mail field intentionally omitted from the request
    }

    @And("my e-mail address is set to null")
    public void myEMailAddressIsNull() {
        this.parentJson.putNull("mail");
    }

    @And("my e-mail address is left blank")
    public void myEMailAddressIsBlank() {
        this.parentJson.put("mail", "");
    }

    @And("my e-mail address contains only whitespace")
    public void myEMailAddressContainsOnlyWhitespace() {
        this.parentJson.put("mail", "   ");
    }

    @And("I already have an account")
    public void iAlreadyHaveAnAccount() {
        //@formatter:off
        final Parent parentEntity = Parent.builder()
                                    .name(this.parentJson.get("name").asString())
                                    .surname(this.parentJson.get("surname").asString())
                                    .mail(this.parentJson.get("mail").asString())
                                    .build();
        //@formatter:on
        this.parentsRepository.save(parentEntity);
    }

    @When("I click on \"Create account\"")
    public void createAccount()
            throws Exception {
        final String content = OBJECT_MAPPER.writeValueAsString(this.parentJson);
        this.scenarioContext.setResultActions(
                this.mvc.perform(post("/v0/parents").contentType(APPLICATION_JSON).content(content)));
    }

    @Then("I receive a confirmation that my account has been successfully created")
    public void accountCreatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

    @Then("I receive an error because the account already exists")
    public void iReceiveAnErrorBecauseTheAccountAlreadyExists()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isConflict());
    }

}
