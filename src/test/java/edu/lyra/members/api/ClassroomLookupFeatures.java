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

public class ClassroomLookupFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("I request the list of classrooms")
    public void requestClassroomList()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/classrooms").with(this.scenarioContext.getJwtProcessor())));
    }

    @When("I request the list of classrooms with page size {int} and page number {int}")
    public void requestClassroomListPaged(final int size, final int page)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get("/v0/classrooms").queryParam("size", String.valueOf(size))
                                     .queryParam("page", String.valueOf(page))
                                     .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request classroom for course {int} group {string}")
    public void requestClassroom(final int course, final String group)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get(this.scenarioContext.getLocation("classroom:" + course + " " + group))
                        .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request a classroom that does not exist")
    public void requestNonExistentClassroom()
            throws Exception {
        this.scenarioContext.setResultActions(this.mvc.perform(
                get("/v0/classrooms/" + UUID.randomUUID()).with(this.scenarioContext.getJwtProcessor())));
    }

    @Then("the list of classrooms includes course {int} group {string}")
    public void listOfClassroomsIncludes(final int course, final String group)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final String expectedLocation = this.scenarioContext.getLocation("classroom:" + course + " " + group);
        final JsonNode classrooms = OBJECT_MAPPER.readTree(response.getContentAsString())
                                                 .path("_embedded")
                                                 .path("classrooms");
        boolean found = false;
        for(final JsonNode classroom : classrooms) {
            final String selfLink = classroom.path("_links").path("self").path("href").asString();
            if(selfLink.endsWith(expectedLocation)) {
                found = true;
                break;
            }
        }
        //@formatter:on
        assertTrue(found, "Expected classroom list to include course " + course + " group " + group);
    }

    @Then("I receive a page of {int} classrooms out of a total of {int}")
    public void receivePageOfClassrooms(final int expectedSize, final int expectedTotal)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
        assertEquals(expectedTotal, body.path("page").path("totalElements").asInt());
        assertEquals(expectedSize, body.path("_embedded").path("classrooms").size());
        //@formatter:on
    }

    @Then("I receive the details of classroom for course {int} group {string}")
    public void receiveClassroomDetails(final int course, final String group)
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
        final String expectedLocation = this.scenarioContext.getLocation("classroom:" + course + " " + group);
        assertTrue(selfLink.endsWith(expectedLocation),
                   "Expected classroom link " + selfLink + " to match " + expectedLocation);
    }

}
