package edu.lyra.members.api.cucumber.kid;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomsRepository;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import edu.lyra.members.api.cucumber.TestSecurityContext;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidsRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentsRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KidLookupFeatures
        extends AbstractResourceFeatures {

    private final Map<Integer, JsonNode> kidPages = new HashMap<>();

    @Autowired
    private KidsRepository kidsRepository;

    @Autowired
    private ParentsRepository parentsRepository;

    @Autowired
    private ClassroomsRepository classroomsRepository;

    @Given("the following kids exist:")
    public void theFollowingKidsExist(final DataTable table) {
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for(final Map<String, String> row : rows) {
            this.aKidExists(row.get("name"), row.get("surname"), row.get("birthdate"), row.get("parent"));
        }
    }

    private void aKidExists(final String name, final String surname, final String birthdate, final String parentName) {
        //@formatter:off
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
                                 .set(field(Kid.class, "parent"), this.parent(parentName))
                                 .create();
        //@formatter:on
        final Kid saved = TestSecurityContext.runAuthenticated(() -> this.kidsRepository.save(kid));
        this.scenarioContext.putLocation("kid:" + name + " " + surname, "/v0/kids/" + saved.getId());
    }

    private Parent parent(final String key) {
        final String location = this.scenarioContext.getLocation("parent:" + key);
        final UUID   id       = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.parentsRepository.findById(id).orElseThrow();
    }

    @Given("kid {string} {string} is enrolled in classroom for course {int} group {string}")
    public void kidIsEnrolledInClassroom(
            final String name,
            final String surname,
            final int course,
            final String group
    ) {
        final Kid kid = this.kid(name, surname);
        kid.setClassroom(this.classroom(course, group));
        TestSecurityContext.runAuthenticated(() -> this.kidsRepository.save(kid));
    }

    private Kid kid(final String name, final String surname) {
        final String location = this.scenarioContext.getLocation("kid:" + name + " " + surname);
        final UUID   id       = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.kidsRepository.findById(id).orElseThrow();
    }

    private Classroom classroom(final int course, final String group) {
        final String location = this.scenarioContext.getLocation("classroom:" + course + " " + group);
        final UUID   id       = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.classroomsRepository.findById(id).orElseThrow();
    }

    @When("I request the list of kids")
    public void requestKidList()
            throws Exception {
        this.perform(get("/v0/kids"));
    }

    @When("I request the list of kids with page size {int}")
    public void requestAllKidPages(final int size)
            throws Exception {
        this.kidPages.clear();
        int page = 0;
        int totalPages;
        do {
            final JsonNode body = this.requestKidPage(size, page);
            this.kidPages.put(page, body);
            totalPages = body.path("page").path("totalPages").asInt();
            page++;
        } while(page < totalPages);
    }

    private JsonNode requestKidPage(final int size, final int page)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.mvc.perform(
                get("/v0/kids").queryParam("size", String.valueOf(size))
                               .queryParam("page", String.valueOf(page))
                               .with(this.scenarioContext.getJwtProcessor()))
                                                          .andExpect(status().isOk())
                                                          .andReturn().getResponse();
        //@formatter:on
        return OBJECT_MAPPER.readTree(response.getContentAsString());
    }

    @When("I request kid {string} {string}")
    public void requestKid(final String name, final String surname)
            throws Exception {
        this.perform(get(this.scenarioContext.getLocation("kid:" + name + " " + surname)));
    }

    @When("I request a kid that does not exist")
    public void requestNonExistentKid()
            throws Exception {
        this.perform(get("/v0/kids/" + UUID.randomUUID()));
    }

    @Then("the list of kids contains exactly the following kids:")
    public void listOfKidsContainsExactly(final DataTable table)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode kids = OBJECT_MAPPER.readTree(response.getContentAsString())
                                           .path("_embedded")
                                           .path("kids");
        //@formatter:on
        this.assertKidsMatch(kids, table.asMaps(String.class, String.class));
    }

    private void assertKidsMatch(final JsonNode kids, final List<Map<String, String>> rows) {
        assertEquals(rows.size(), kids.size(), "Expected kid list to contain exactly %d kids".formatted(rows.size()));
        for(final Map<String, String> row : rows) {
            final String name     = row.get("name");
            final String surname  = row.get("surname");
            final String location = this.scenarioContext.getLocation("kid:" + name + " " + surname);
            boolean      found    = false;
            for(final JsonNode kid : kids) {
                final String selfLink = kid.path("_links").path("self").path("href").asString();
                if(selfLink.endsWith(location)) {
                    found = true;
                    assertEquals(name, kid.path("name").asString());
                    assertEquals(surname, kid.path("surname").asString());
                    assertEquals(row.get("birthdate"), kid.path("birthdate").asString());
                    break;
                }
            }
            assertTrue(found, "Expected kid list to include %s %s".formatted(name, surname));
        }
    }

    @Then("page {int} contains exactly the following kids:")
    public void pageContainsExactly(final int pageNumber, final DataTable table) {
        final JsonNode kids = this.kidPages.get(pageNumber).path("_embedded").path("kids");
        this.assertKidsMatch(kids, table.asMaps(String.class, String.class));
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
        final String location = this.scenarioContext.getLocation("kid:" + name + " " + surname);
        assertTrue(selfLink.endsWith(location), "Expected kid link %s to match %s".formatted(selfLink, location));
    }

}
