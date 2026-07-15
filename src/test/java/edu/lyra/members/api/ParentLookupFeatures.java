package edu.lyra.members.api;

import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.ContactInfo;
import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import io.cucumber.java.en.Given;
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

public class ParentLookupFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ParentsRepository parentsRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    @Given("parent {string} {string} exists with e-mail {string}")
    public void parentExistsWithMail(final String name, final String surname, final String mail) {
        //@formatter:off
        final Parent parent = Parent.builder()
                                    .id(UUID.randomUUID())
                                    .contactInfo(ContactInfo.builder().name(name).surname(surname).mail(mail).build())
                                    .build();
        //@formatter:on
        final Parent saved = TestSecurityContext.runAuthenticated(() -> this.parentsRepository.save(parent));
        this.scenarioContext.putLocation("parent:" + name + " " + surname, "/v0/parents/" + saved.getId());
    }

    @When("I request the list of parents")
    public void requestParentList()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/parents").with(this.scenarioContext.getJwtProcessor())));
    }

    @When("I request the list of parents with page size {int} and page number {int}")
    public void requestParentListPaged(final int size, final int page)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get("/v0/parents").queryParam("size", String.valueOf(size))
                                  .queryParam("page", String.valueOf(page))
                                  .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request parent {string} {string}")
    public void requestParent(final String name, final String surname)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get(this.scenarioContext.getLocation("parent:" + name + " " + surname))
                        .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request a parent that does not exist")
    public void requestNonExistentParent()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/parents/" + UUID.randomUUID()).with(this.scenarioContext.getJwtProcessor())));
    }

    @Then("the list of parents includes {string} {string}")
    public void listOfParentsIncludes(final String name, final String surname)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final String expectedLocation = this.scenarioContext.getLocation("parent:" + name + " " + surname);
        final JsonNode parents = OBJECT_MAPPER.readTree(response.getContentAsString())
                                              .path("_embedded")
                                              .path("parents");
        boolean found = false;
        for(final JsonNode parent : parents) {
            final String selfLink = parent.path("_links").path("self").path("href").asString();
            if(selfLink.endsWith(expectedLocation)) {
                found = true;
                break;
            }
        }
        //@formatter:on
        assertTrue(found, "Expected parent list to include " + name + " " + surname);
    }

    @Then("I receive a page of {int} parents out of a total of {int}")
    public void receivePageOfParents(final int expectedSize, final int expectedTotal)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
        assertEquals(expectedTotal, body.path("page").path("totalElements").asInt());
        assertEquals(expectedSize, body.path("_embedded").path("parents").size());
        //@formatter:on
    }

    @Then("I receive the details of parent {string} {string}")
    public void receiveParentDetails(final String name, final String surname)
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
        final String expectedLocation = this.scenarioContext.getLocation("parent:" + name + " " + surname);
        assertTrue(selfLink.endsWith(expectedLocation),
                   "Expected parent link " + selfLink + " to match " + expectedLocation);
    }

}
