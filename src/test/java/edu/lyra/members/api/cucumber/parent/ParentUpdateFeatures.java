package edu.lyra.members.api.cucumber.parent;

import java.time.LocalDate;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import edu.lyra.members.api.cucumber.TestSecurityContext;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.node.ObjectNode;

import static java.time.Month.DECEMBER;

import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ParentUpdateFeatures
        extends AbstractResourceFeatures {

    private static final MediaType URI_LIST = MediaType.parseMediaType("text/uri-list");

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private KidRepository kidRepository;

    @When("I update parent {string} {string}'s surname to {string}")
    public void updateParentSurname(final String name, final String surname, final String newSurname)
            throws Exception {
        this.performUpdateSurname(this.scenarioContext.getLocation("parent:" + name + " " + surname), newSurname);
    }

    private void performUpdateSurname(final String location, final String newSurname)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("surname", newSurname);
        this.performWithBody(patch(location), body);
    }

    @When("I update a parent that does not exist")
    public void updateNonExistentParent()
            throws Exception {
        this.performUpdateSurname("/v0/parents/" + UUID.randomUUID(), "Doesn't matter");
    }

    @Then("I receive a confirmation that the account has been successfully updated")
    public void accountUpdatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

    @Given("kid {string} {string} was created by parent {string} and is not yet bound to any parent")
    public void kidCreatedByParentUnbound(final String name, final String surname, final String creatorParentName) {
        final Parent creator = this.parent(creatorParentName);
        //@formatter:off
        final Kid kid = Instancio.of(Kid.class)
                                 .ignore(field(Kid.class, "id"))
                                 .ignore(field(Auditable.class, "version"))
                                 .ignore(field(Auditable.class, "createdDate"))
                                 .ignore(field(Auditable.class, "createdBy"))
                                 .ignore(field(Auditable.class, "lastModifiedDate"))
                                 .ignore(field(Auditable.class, "updatedBy"))
                                 .set(field(Kid.class, "name"), name)
                                 .set(field(Kid.class, "surname"), surname)
                                 .set(field(Kid.class, "birthdate"), LocalDate.of(2019, DECEMBER, 12))
                                 .set(field(Kid.class, "parent"), (Parent) null)
                                 .set(field(Kid.class, "classroom"), (Classroom) null)
                                 .create();
        final Kid saved = TestSecurityContext.runAuthenticated(creator.getId(), () -> this.kidRepository.save(kid));
        //@formatter:on
        this.scenarioContext.putLocation("kid:" + name + " " + surname, "/v0/kids/" + saved.getId());
    }

    private Parent parent(final String key) {
        final String location = this.scenarioContext.getLocation("parent:" + key);
        final UUID   id       = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.parentRepository.findById(id).orElseThrow();
    }

    @When("I bind kid {string} {string} to parent {string}")
    public void bindKidToParent(final String kidName, final String kidSurname, final String parentName)
            throws Exception {
        final String parentLocation = this.scenarioContext.getLocation("parent:" + parentName);
        final String kidLocation    = this.scenarioContext.getLocation("kid:" + kidName + " " + kidSurname);
        this.perform(post(parentLocation + "/kids").contentType(URI_LIST).content(kidLocation));
    }

    @Then("I receive a confirmation that the kid has been successfully bound to the parent")
    public void kidBoundOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
