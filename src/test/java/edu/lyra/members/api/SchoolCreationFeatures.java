package edu.lyra.members.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SchoolCreationFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern REPEAT_PATTERN = Pattern.compile("^([a-zA-Z])\\((\\d+)\\)(.*)$");

    private final ObjectNode school = OBJECT_MAPPER.createObjectNode();

    private ResultActions resultActions;

    @Autowired
    private MockMvc mvc;

    @Before
    public void cleanJson() {
        this.school.removeAll();
    }

    @Given("the school name is {string}")
    public void schoolNameIs(final String nameToken) {
        this.putMaybe(nameToken);
    }

    @When("I click on \"Create school\"")
    public void createSchool()
            throws Exception {
        final String content = OBJECT_MAPPER.writeValueAsString(this.school);
        this.resultActions = this.mvc.perform(post("/v0/schools").contentType(APPLICATION_JSON).content(content));
    }

    @Then("I receive a confirmation that the school has been successfully created")
    public void schoolCreatedOk()
            throws Exception {
        this.resultActions.andExpect(status().isCreated());
    }

    @Then("I receive an error because the school data is invalid")
    public void schoolCreationBadRequest()
            throws Exception {
        this.resultActions.andExpect(status().isBadRequest());
    }

    private void putMaybe(String token) {
        if(token == null) {
            this.school.putNull("name");
            return;
        }
        switch(token) {
            case "missing":
                return; // omit field
            case "null":
                this.school.putNull("name");
                return;
            case "empty":
                this.school.put("name", "");
                return;
            case "spaces":
                this.school.put("name", "   ");
                return;
            default:
                this.school.put("name", expand(token));
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
