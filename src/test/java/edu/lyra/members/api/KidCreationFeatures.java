package edu.lyra.members.api;

import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KidCreationFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectNode kidJson = OBJECT_MAPPER.createObjectNode();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ParentsRepository parentsRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    @Before
    public void cleanKidJson() {
        this.kidJson.removeAll();
    }

    @And("the kid name is {string}")
    public void kidNameIs(final String name) {
        this.kidJson.put("name", name);
    }

    @And("the kid surname is {string}")
    public void kidSurnameIs(final String surname) {
        this.kidJson.put("surname", surname);
    }

    @And("the kid birth date is {string}")
    public void kidBirthDateIs(final String birthdate) {
        this.kidJson.put("birthdate", birthdate);
    }

    @When("I add the kid to my account")
    public void addKid() throws Exception {
        final int parentId = parentsRepository.findByMail(scenarioContext.getParentSub()).orElseThrow().getId();
        this.kidJson.put("parent", "http://localhost/v0/parents/" + parentId);
        this.scenarioContext.setResultActions(
                this.mvc.perform(post("/v0/kids").with(this.scenarioContext.getJwtProcessor())
                        .contentType(APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(this.kidJson))));
    }

    @Then("I receive a confirmation that the kid has been successfully added")
    public void kidAddedOk() throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

}
