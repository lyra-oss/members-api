package edu.lyra.members.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.cucumber.datatable.DataTable;
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

public class SchoolLookupFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("I request the list of schools")
    public void requestSchoolList()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/schools").with(this.scenarioContext.getJwtProcessor())));
    }

    @When("I request the list of schools with page size {int} and page number {int}")
    public void requestSchoolListPaged(final int size, final int page)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get("/v0/schools").queryParam("size", String.valueOf(size))
                                  .queryParam("page", String.valueOf(page))
                                  .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request school {string}")
    public void requestSchool(final String name)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get(this.scenarioContext.getLocation("school:" + name)).with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request a school that does not exist")
    public void requestNonExistentSchool()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/schools/" + UUID.randomUUID()).with(this.scenarioContext.getJwtProcessor())));
    }

    @Then("the list of schools contains exactly the following schools:")
    public void listOfSchoolsContainsExactly(final DataTable table)
            throws Exception {
        //@formatter:off
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode schools = OBJECT_MAPPER.readTree(response.getContentAsString())
                                              .path("_embedded")
                                              .path("schools");
        //@formatter:on
        assertEquals(rows.size(), schools.size(),
                     "Expected school list to contain exactly %d schools".formatted(rows.size()));
        for(final Map<String, String> row : rows) {
            final String name     = row.get("name");
            final String location = this.scenarioContext.getLocation("school:" + name);
            boolean      found    = false;
            for(final JsonNode school : schools) {
                final String selfLink = school.path("_links").path("self").path("href").asString();
                if(selfLink.endsWith(location)) {
                    found = true;
                    assertEquals(name, school.path("name").asString());
                    break;
                }
            }
            assertTrue(found, "Expected school list to include %s".formatted(name));
        }
    }

    @Then("I receive a page of {int} schools out of a total of {int}")
    public void receivePageOfSchools(final int expectedSize, final int expectedTotal)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
        assertEquals(expectedTotal, body.path("page").path("totalElements").asInt());
        assertEquals(expectedSize, body.path("_embedded").path("schools").size());
        //@formatter:on
    }

    @Then("I receive the details of school {string}")
    public void receiveSchoolDetails(final String name)
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
        final String location = this.scenarioContext.getLocation("school:" + name);
        assertTrue(selfLink.endsWith(location), "Expected school link %s to match %s".formatted(selfLink, location));
    }

}
