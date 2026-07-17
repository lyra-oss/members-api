package edu.lyra.members.api.cucumber.classroom;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClassroomLookupFeatures
        extends AbstractResourceFeatures {

    @When("I request the list of classrooms")
    public void requestClassroomList()
            throws Exception {
        this.perform(get("/v0/classrooms"));
    }

    @When("I request the list of classrooms with page size {int} and page number {int}")
    public void requestClassroomListPaged(final int size, final int page)
            throws Exception {
        //@formatter:off
        this.perform(get("/v0/classrooms").queryParam("size", String.valueOf(size))
                                          .queryParam("page", String.valueOf(page)));
        //@formatter:on
    }

    @When("I request classroom for course {int} group {string}")
    public void requestClassroom(final int course, final String group)
            throws Exception {
        this.perform(get(this.scenarioContext.getLocation("classroom:" + course + " " + group)));
    }

    @When("I request a classroom that does not exist")
    public void requestNonExistentClassroom()
            throws Exception {
        this.perform(get("/v0/classrooms/" + UUID.randomUUID()));
    }

    @Then("the list of classrooms contains exactly the following classrooms:")
    public void listOfClassroomsContainsExactly(final DataTable table)
            throws Exception {
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn()
                                                                     .getResponse();
        final JsonNode classrooms = OBJECT_MAPPER.readTree(response.getContentAsString())
                                                 .path("_embedded")
                                                 .path("classrooms");
        //@formatter:on
        assertEquals(rows.size(), classrooms.size(),
                     "Expected classroom list to contain exactly %d classrooms".formatted(rows.size()));
        for(final Map<String, String> row : rows) {
            final int    course   = Integer.parseInt(row.get("course"));
            final String group    = row.get("group");
            final String location = this.scenarioContext.getLocation("classroom:" + course + " " + group);
            boolean      found    = false;
            for(final JsonNode classroom : classrooms) {
                final String selfLink = classroom.path("_links").path("self").path("href").asString();
                if(selfLink.endsWith(location)) {
                    found = true;
                    assertEquals(course, classroom.path("course").asInt());
                    assertEquals(group, classroom.path("group").asString());
                    break;
                }
            }
            assertTrue(found, "Expected classroom list to include course %d group %s".formatted(course, group));
        }
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
                   "Expected classroom link %s to match %s".formatted(selfLink, expectedLocation));
    }

}
