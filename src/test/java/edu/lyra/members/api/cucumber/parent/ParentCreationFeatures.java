package edu.lyra.members.api.cucumber.parent;

import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ParentCreationFeatures
        extends AbstractResourceFeatures {

    private final ObjectNode body = OBJECT_MAPPER.createObjectNode();

    @Autowired
    private KidRepository kidRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Before(order = - 10)
    public void cleanRepositories() {
        this.body.removeAll();
        this.kidRepository.deleteAll();
        this.parentRepository.deleteAll();
    }

    @Given("my name is {string}")
    public void myNameIs(final String name) {
        this.body.put("name", name);
    }

    @Given("my name is longer than 100 characters")
    public void myNameTooLong() {
        this.body.put("name", "a".repeat(101));
    }

    @Given("my name is not provided")
    public void myNameNotProvided() {
        // name field intentionally omitted from the request
    }

    @Given("my name is set to null")
    public void myNameIsNull() {
        this.body.putNull("name");
    }

    @Given("my name is left blank")
    public void myNameIsBlank() {
        this.body.put("name", "");
    }

    @Given("my name contains only whitespace")
    public void myNameContainsOnlyWhitespace() {
        this.body.put("name", "   ");
    }

    @And("my surname is {string}")
    public void mySurnameIs(final String surname) {
        this.body.put("surname", surname);
    }

    @And("my surname is longer than 100 characters")
    public void mySurnameTooLong() {
        this.body.put("surname", "b".repeat(101));
    }

    @And("my surname is not provided")
    public void mySurnameNotProvided() {
        // surname field intentionally omitted from the request
    }

    @And("my surname is set to null")
    public void mySurnameIsNull() {
        this.body.putNull("surname");
    }

    @And("my surname is left blank")
    public void mySurnameIsBlank() {
        this.body.put("surname", "");
    }

    @And("my surname contains only whitespace")
    public void mySurnameContainsOnlyWhitespace() {
        this.body.put("surname", "   ");
    }

    @And("my e-mail address is {string}")
    public void myEMailAddressIs(final String mail) {
        this.body.put("mail", mail);
    }

    @And("my e-mail address is longer than 200 characters")
    public void myEMailAddressTooLong() {
        this.body.put("mail", "c".repeat(201) + "@example.com");
    }

    @And("my e-mail address is not provided")
    public void myEMailAddressNotProvided() {
        // mail field intentionally omitted from the request
    }

    @And("my e-mail address is set to null")
    public void myEMailAddressIsNull() {
        this.body.putNull("mail");
    }

    @And("my e-mail address is left blank")
    public void myEMailAddressIsBlank() {
        this.body.put("mail", "");
    }

    @And("my e-mail address contains only whitespace")
    public void myEMailAddressContainsOnlyWhitespace() {
        this.body.put("mail", "   ");
    }

    @And("I already have an account")
    public void iAlreadyHaveAnAccount() {
        //@formatter:on
        final UUID subject = UUID.randomUUID();
        final Person person = Person.builder().id(subject).name(this.body.get("name").asString())
                                    .surname(this.body.get("surname").asString()).mail(this.body.get("mail").asString())
                                    .build();
        final Parent parentEntity = Parent.builder().person(person).build();
        //@formatter:on
        this.saveAsSelf(subject, parentEntity);
    }

    private void saveAsSelf(final UUID subject, final Parent parent) {
        final Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "none")
                           .subject(subject.toString())
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        try {
            this.parentRepository.save(parent);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }

    @When("I click on \"Create account\"")
    public void createAccount()
            throws Exception {
        this.performWithBody(post("/v0/parents"), this.body);
    }

    @Then("I receive a confirmation that my account has been successfully created")
    public void accountCreatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

    @Then("I receive an error because the account already exists")
    public void iReceiveAnErrorBecauseTheAccountAlreadyExists()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isConflict());
    }

}