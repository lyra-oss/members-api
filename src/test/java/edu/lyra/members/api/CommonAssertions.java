package edu.lyra.members.api;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommonAssertions {

    @Autowired
    private ScenarioContext scenarioContext;

    @Then("I receive the error {string}")
    public void receiveError(final String expectedMessage)
            throws Exception {
        this.scenarioContext.getResultActions()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].message", hasItem(expectedMessage)));
    }

}
