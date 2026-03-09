package com.sagittec.lyra.members.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.sagittec.lyra.members.api.repositories.jpa.Parent;
import com.sagittec.lyra.members.api.repositories.jpa.ParentsRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ParentCreationFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern REPEAT_PATTERN = Pattern.compile("^([a-zA-Z])\\((\\d+)\\)(.*)$");

    private final ObjectNode parentJson = OBJECT_MAPPER.createObjectNode();

    private ResultActions resultActions;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ParentsRepository parentsRepository;

    @Before
    public void cleanJson() {
        this.parentJson.removeAll();
        this.parentsRepository.deleteAll();
    }

    @Given("my name is {string}")
    public void myNameIs(final String nameToken) {
        this.putMaybe("name", nameToken);
    }

    @And("my surname is {string}")
    public void mySurnameIs(final String surnameToken) {
        this.putMaybe("surname", surnameToken);
    }

    @And("my e-mail address is {string}")
    public void myEMailAddressIs(final String mailToken) {
        this.putMaybe("mail", mailToken);
    }

    @And("I already have an account")
    public void iAlreadyHaveAnAccount() {
        //@formatter:off
        final Parent parentEntity = Parent.builder()
                                    .name(this.parentJson.get("name").asText())
                                    .surname(this.parentJson.get("surname").asText())
                                    .mail(this.parentJson.get("mail").asText())
                                    .build();
        //@formatter:on
        this.parentsRepository.save(parentEntity);
    }

    @When("I click on \"Create account\"")
    public void createAccount()
            throws Exception {
        final String content = OBJECT_MAPPER.writeValueAsString(this.parentJson);
        this.resultActions = this.mvc.perform(post("/v0/parents").contentType(APPLICATION_JSON).content(content));
    }

    @Then("I receive a confirmation that my account has been successfully created")
    public void accountCreatedOk()
            throws Exception {
        this.resultActions.andExpect(status().isCreated());
    }

    @Then("I receive an error because my data is invalid")
    public void accountCreationBadRequest()
            throws Exception {
        this.resultActions.andExpect(status().isBadRequest());
    }

    @Then("I receive an error because the account already exists")
    public void iReceiveAnErrorBecauseTheAccountAlreadyExists()
            throws Exception {
        this.resultActions.andExpect(status().isConflict());
    }

    private void putMaybe(String field, String token) {
        if(token == null) {
            this.parentJson.putNull(field);
            return;
        }
        switch(token) {
            case "missing":
                // Do not include this field in the payload
                return;
            case "null":
                this.parentJson.putNull(field);
                return;
            case "empty":
                this.parentJson.put(field, "");
                return;
            case "spaces":
                this.parentJson.put(field, "   ");
                return;
            default:
                this.parentJson.put(field, expand(token));
        }
    }

    private String expand(String token) {
        final Matcher m = REPEAT_PATTERN.matcher(token);
        if(m.matches()) {
            char         ch     = m.group(1).charAt(0);
            int          count  = Integer.parseInt(m.group(2));
            final String suffix = m.group(3) == null ? "" : m.group(3);
            return ("" + ch).repeat(Math.max(0, count)) + suffix;
        }
        return token;
    }

}
