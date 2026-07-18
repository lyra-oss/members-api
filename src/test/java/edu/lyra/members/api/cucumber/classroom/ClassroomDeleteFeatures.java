package edu.lyra.members.api.cucumber.classroom;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClassroomDeleteFeatures
        extends AbstractResourceFeatures {

    @When("I delete the classroom")
    public void deleteClassroom()
            throws Exception {
        this.perform(delete(this.scenarioContext.getLocation("classroom")));
    }

    @When("I delete a classroom that does not exist")
    public void deleteNonExistentClassroom()
            throws Exception {
        this.perform(delete("/v0/classrooms/" + UUID.randomUUID()));
    }

    @Then("I receive a confirmation that the classroom has been successfully deleted")
    public void classroomDeletedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
