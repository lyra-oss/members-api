package edu.lyra.members.api;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommonAssertions {

    private static final String ERROR_MESSAGE = "$.errors[?(@.property == '%s' && @.message == '%s')]";

    @Autowired
    private ScenarioContext scenarioContext;

    @Then("I receive an error stating that {string} field is incorrect because {string}")
    public void receiveErrorForField(final String expectedField, final String expectedMessage)
            throws Exception {
        this.scenarioContext.getResultActions()
                            .andExpect(status().isBadRequest())
                            .andExpect(jsonPath(ERROR_MESSAGE.formatted(expectedField, expectedMessage)).exists());
    }

    @Then("I receive a forbidden error")
    public void receiveForbiddenError()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isForbidden());
    }

    @Then("I receive a not found error")
    public void receiveNotFoundError()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNotFound());
    }

}
