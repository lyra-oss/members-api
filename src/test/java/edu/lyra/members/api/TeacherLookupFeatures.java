package edu.lyra.members.api;

import java.util.UUID;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TeacherLookupFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("I request the list of teachers")
    public void requestTeacherList()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/teachers").with(this.scenarioContext.getJwtProcessor())));
    }

    @When("I request the list of teachers with page size {int} and page number {int}")
    public void requestTeacherListPaged(final int size, final int page)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get("/v0/teachers").queryParam("size", String.valueOf(size))
                                   .queryParam("page", String.valueOf(page))
                                   .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request teacher {string} {string}")
    public void requestTeacher(final String name, final String surname)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get(this.scenarioContext.getLocation("teacher:" + name + " " + surname))
                        .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request a teacher that does not exist")
    public void requestNonExistentTeacher()
            throws Exception {
        this.scenarioContext.setResultActions(this.mvc.perform(
                get("/v0/teachers/" + UUID.randomUUID()).with(this.scenarioContext.getJwtProcessor())));
    }

    @Then("the list of teachers includes {string} {string}")
    public void listOfTeachersIncludes(final String name, final String surname)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final String expectedLocation = this.scenarioContext.getLocation("teacher:" + name + " " + surname);
        final JsonNode teachers = OBJECT_MAPPER.readTree(response.getContentAsString())
                                               .path("_embedded")
                                               .path("teachers");
        boolean found = false;
        for(final JsonNode teacher : teachers) {
            final String selfLink = teacher.path("_links").path("self").path("href").asString();
            if(selfLink.endsWith(expectedLocation)) {
                found = true;
                break;
            }
        }
        //@formatter:on
        assertTrue(found, "Expected teacher list to include " + name + " " + surname);
    }

    @Then("I receive a page of {int} teachers out of a total of {int}")
    public void receivePageOfTeachers(final int expectedSize, final int expectedTotal)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
        assertEquals(expectedTotal, body.path("page").path("totalElements").asInt());
        assertEquals(expectedSize, body.path("_embedded").path("teachers").size());
        //@formatter:on
    }

    @Then("I receive the details of teacher {string} {string}")
    public void receiveTeacherDetails(final String name, final String surname)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final String selfLink = OBJECT_MAPPER.readTree(response.getContentAsString())
                                             .path("_links")
                                             .path("self")
                                             .path("href")
                                             .asString();
        //@formatter:on
        final String expectedLocation = this.scenarioContext.getLocation("teacher:" + name + " " + surname);
        assertTrue(selfLink.endsWith(expectedLocation),
                   "Expected teacher link " + selfLink + " to match " + expectedLocation);
    }

}
