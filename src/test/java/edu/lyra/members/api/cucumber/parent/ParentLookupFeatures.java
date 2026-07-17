package edu.lyra.members.api.cucumber.parent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import edu.lyra.members.api.cucumber.TestSecurityContext;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ParentLookupFeatures
        extends AbstractResourceFeatures {

    @Autowired
    private ParentRepository parentRepository;

    public void parentExistsWithMail(final String name, final String surname, final String mail) {
        final Person person = Person.builder().id(UUID.randomUUID()).name(name).surname(surname).mail(mail).build();
        final Parent parent = Parent.builder().person(person).build();
        final Parent saved  = TestSecurityContext.runAuthenticated(() -> this.parentRepository.save(parent));
        this.scenarioContext.putLocation("parent:" + name + " " + surname, "/v0/parents/" + saved.getId());
    }

    @Given("the following parents exist:")
    public void theFollowingParentsExist(final DataTable table) {
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for(final Map<String, String> row : rows) {
            this.parentExistsWithMail(row.get("name"), row.get("surname"), row.get("mail"));
        }
    }

    @When("I request the list of parents")
    public void requestParentList()
            throws Exception {
        this.perform(get("/v0/parents"));
    }

    @When("I request the list of parents with page size {int} and page number {int}")
    public void requestParentListPaged(final int size, final int page)
            throws Exception {
        //@formatter:off
        this.perform(get("/v0/parents").queryParam("size", String.valueOf(size))
                                       .queryParam("page", String.valueOf(page)));
        //@formatter:on
    }

    @When("I request parent {string} {string}")
    public void requestParent(final String name, final String surname)
            throws Exception {
        this.perform(get(this.scenarioContext.getLocation("parent:" + name + " " + surname)));
    }

    @When("I request a parent that does not exist")
    public void requestNonExistentParent()
            throws Exception {
        this.perform(get("/v0/parents/" + UUID.randomUUID()));
    }

    @Then("the list of parents contains exactly the following parents:")
    public void listOfParentsContainsExactly(final DataTable table)
            throws Exception {
        //@formatter:off
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                                                                     .andExpect(status().isOk())
                                                                     .andReturn().getResponse();
        final JsonNode parents = OBJECT_MAPPER.readTree(response.getContentAsString())
                                              .path("_embedded")
                                              .path("parents");
        //@formatter:on
        assertEquals(rows.size(), parents.size(),
                     "Expected parent list to contain exactly %d parents".formatted(rows.size()));
        for(final Map<String, String> row : rows) {
            final String name     = row.get("name");
            final String surname  = row.get("surname");
            final String location = this.scenarioContext.getLocation("parent:" + name + " " + surname);
            boolean      found    = false;
            for(final JsonNode parent : parents) {
                final String selfLink = parent.path("_links").path("self").path("href").asString();
                if(selfLink.endsWith(location)) {
                    found = true;
                    assertEquals(name, parent.path("name").asString());
                    assertEquals(surname, parent.path("surname").asString());
                    assertEquals(row.get("mail"), parent.path("mail").asString());
                    break;
                }
            }
            assertTrue(found, "Expected parent list to include %s %s".formatted(name, surname));
        }
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
        final String location = this.scenarioContext.getLocation("parent:" + name + " " + surname);
        assertTrue(selfLink.endsWith(location), "Expected parent link %s to match %s".formatted(selfLink, location));
    }

}
