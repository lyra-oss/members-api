package edu.lyra.members.api;

import java.time.LocalDate;
import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Auditable;
import edu.lyra.members.api.repositories.jpa.ContactInfo;
import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.KidsRepository;
import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KidLookupFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private KidsRepository kidsRepository;

    @Autowired
    private ParentsRepository parentsRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    @Given("a kid named {string} {string} born on {string} exists")
    public void aKidExists(final String name, final String surname, final String birthdate) {
        //@formatter:off
        final Parent parent = Parent.builder()
                                    .id(UUID.randomUUID())
                                    .contactInfo(ContactInfo.builder()
                                                            .name("Auto")
                                                            .surname("Parent")
                                                            .mail(UUID.randomUUID() + "@example.com")
                                                            .build())
                                    .build();
        final Parent savedParent = TestSecurityContext.runAuthenticated(() -> this.parentsRepository.save(parent));
        final Kid kid = Instancio.of(Kid.class)
                                 .ignore(field(Kid.class, "id"))
                                 .ignore(field(Kid.class, "classroom"))
                                 .ignore(field(Auditable.class, "version"))
                                 .ignore(field(Auditable.class, "createdDate"))
                                 .ignore(field(Auditable.class, "createdBy"))
                                 .ignore(field(Auditable.class, "lastModifiedDate"))
                                 .ignore(field(Auditable.class, "updatedBy"))
                                 .set(field(Kid.class, "name"), name)
                                 .set(field(Kid.class, "surname"), surname)
                                 .set(field(Kid.class, "birthdate"), LocalDate.parse(birthdate))
                                 .set(field(Kid.class, "parent"), savedParent)
                                 .create();
        //@formatter:on
        final Kid saved = TestSecurityContext.runAuthenticated(() -> this.kidsRepository.save(kid));
        this.scenarioContext.putLocation("kid:" + name + " " + surname, "/v0/kids/" + saved.getId());
    }

    @When("I request the list of kids")
    public void requestKidList()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/kids").with(this.scenarioContext.getJwtProcessor())));
    }

    @When("I request the list of kids with page size {int} and page number {int}")
    public void requestKidListPaged(final int size, final int page)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get("/v0/kids").queryParam("size", String.valueOf(size))
                               .queryParam("page", String.valueOf(page))
                               .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request kid {string} {string}")
    public void requestKid(final String name, final String surname)
            throws Exception {
        //@formatter:off
        this.scenarioContext.setResultActions(this.mvc.perform(
                get(this.scenarioContext.getLocation("kid:" + name + " " + surname))
                        .with(this.scenarioContext.getJwtProcessor())));
        //@formatter:on
    }

    @When("I request a kid that does not exist")
    public void requestNonExistentKid()
            throws Exception {
        this.scenarioContext.setResultActions(
                this.mvc.perform(get("/v0/kids/" + UUID.randomUUID()).with(this.scenarioContext.getJwtProcessor())));
    }

    @Then("the list of kids includes {string} {string}")
    public void listOfKidsIncludes(final String name, final String surname)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final String expectedLocation = this.scenarioContext.getLocation("kid:" + name + " " + surname);
        final JsonNode kids = OBJECT_MAPPER.readTree(response.getContentAsString())
                                           .path("_embedded")
                                           .path("kids");
        boolean found = false;
        for(final JsonNode kid : kids) {
            final String selfLink = kid.path("_links").path("self").path("href").asString();
            if(selfLink.endsWith(expectedLocation)) {
                found = true;
                break;
            }
        }
        //@formatter:on
        assertTrue(found, "Expected kid list to include " + name + " " + surname);
    }

    @Then("I receive a page of {int} kids out of a total of {int}")
    public void receivePageOfKids(final int expectedSize, final int expectedTotal)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode body = OBJECT_MAPPER.readTree(response.getContentAsString());
        assertEquals(expectedTotal, body.path("page").path("totalElements").asInt());
        assertEquals(expectedSize, body.path("_embedded").path("kids").size());
        //@formatter:on
    }

    @Then("I receive the details of kid {string} {string}")
    public void receiveKidDetails(final String name, final String surname)
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
        final String expectedLocation = this.scenarioContext.getLocation("kid:" + name + " " + surname);
        assertTrue(selfLink.endsWith(expectedLocation),
                   "Expected kid link " + selfLink + " to match " + expectedLocation);
    }

}
