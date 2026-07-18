package edu.lyra.members.api.cucumber.teacher;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TeacherDeleteFeatures
        extends AbstractResourceFeatures {

    @When("I delete teacher {string} {string}")
    public void deleteTeacher(final String name, final String surname)
            throws Exception {
        this.perform(delete(this.scenarioContext.getLocation("teacher:" + name + " " + surname)));
    }

    @When("I delete a teacher that does not exist")
    public void deleteNonExistentTeacher()
            throws Exception {
        this.perform(delete("/v0/teachers/" + UUID.randomUUID()));
    }

    @Then("I receive a confirmation that the teacher account has been successfully deleted")
    public void teacherDeletedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
