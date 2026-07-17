package edu.lyra.members.api.cucumber.teacher;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TeacherUpdateFeatures
        extends AbstractResourceFeatures {

    @When("I update teacher {string} {string}'s surname to {string}")
    public void updateTeacherSurname(final String name, final String surname, final String newSurname)
            throws Exception {
        this.performUpdateSurname(this.scenarioContext.getLocation("teacher:" + name + " " + surname), newSurname);
    }

    private void performUpdateSurname(final String location, final String newSurname)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("surname", newSurname);
        this.performWithBody(patch(location), body);
    }

    @When("I update a teacher that does not exist")
    public void updateNonExistentTeacher()
            throws Exception {
        this.performUpdateSurname("/v0/teachers/" + UUID.randomUUID(), "Doesn't matter");
    }

    @Then("I receive a confirmation that the teacher account has been successfully updated")
    public void teacherAccountUpdatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
