package com.sagittec.lyra.members.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KidFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern REPEAT_PATTERN = Pattern.compile("^([a-zA-Z])\\((\\d+)\\)(.*)$");

    private final ObjectNode kid = OBJECT_MAPPER.createObjectNode();

    private ResultActions resultActions;

    @Autowired
    private MockMvc mvc;

    @Before
    public void cleanJson() {
        this.kid.removeAll();
    }

    @Given("the kid name is {string}")
    public void kidNameIs(final String nameToken) {
        this.putMaybe("name", nameToken);
    }

    @And("the kid surname is {string}")
    public void kidSurnameIs(final String surnameToken) {
        this.putMaybe("surname", surnameToken);
    }

    @And("the kid birthdate is {string}")
    public void kidBirthdateIs(final String birthdateToken) {
        if (birthdateToken == null) {
            this.kid.putNull("birthdate");
            return;
        }
        switch (birthdateToken) {
            case "missing":
                return; // omit field
            case "null":
                this.kid.putNull("birthdate");
                return;
            case "empty":
                this.kid.put("birthdate", "");
                return;
            case "spaces":
                this.kid.put("birthdate", "   ");
                return;
            case "future":
                this.kid.put("birthdate", LocalDate.now().plusDays(1).toString());
                return;
            case "valid":
                this.kid.put("birthdate", LocalDate.now().minusYears(5).toString());
                return;
            default:
                this.kid.put("birthdate", birthdateToken);
        }
    }

    @When("I click on \"Create kid\"")
    public void createKid() throws Exception {
        final String content = OBJECT_MAPPER.writeValueAsString(this.kid);
        this.resultActions = this.mvc.perform(post("/kids").contentType(APPLICATION_JSON).content(content));
    }

    @Then("I receive a confirmation that the kid has been successfully created")
    public void kidCreatedOk() throws Exception {
        this.resultActions.andExpect(status().isCreated());
    }

    @Then("I receive an error because the kid data is invalid")
    public void kidCreationBadRequest() throws Exception {
        this.resultActions.andExpect(status().isBadRequest());
    }

    private void putMaybe(String field, String token) {
        if (token == null) {
            this.kid.putNull(field);
            return;
        }
        switch (token) {
            case "missing":
                return; // do not include field
            case "null":
                this.kid.putNull(field);
                return;
            case "empty":
                this.kid.put(field, "");
                return;
            case "spaces":
                this.kid.put(field, "   ");
                return;
            default:
                this.kid.put(field, expand(token));
        }
    }

    private String expand(String token) {
        final Matcher m = REPEAT_PATTERN.matcher(token);
        if (m.matches()) {
            char ch = m.group(1).charAt(0);
            int count = Integer.parseInt(m.group(2));
            final String suffix = m.group(3) == null ? "" : m.group(3);
            return ("" + ch).repeat(Math.max(0, count)) + suffix;
        }
        return token;
    }
}
