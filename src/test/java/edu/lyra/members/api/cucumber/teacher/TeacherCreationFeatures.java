package edu.lyra.members.api.cucumber.teacher;

import java.util.UUID;

import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import edu.lyra.members.api.cucumber.EntityFixtures;
import edu.lyra.members.api.cucumber.TestSecurityContext;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TeacherCreationFeatures
        extends AbstractResourceFeatures {

    private final ObjectNode body = OBJECT_MAPPER.createObjectNode();

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Before
    public void cleanTeachers() {
        this.body.removeAll();
        this.teacherRepository.deleteAll();
    }

    @Given("the teacher's name is {string}")
    public void theTeachersNameIs(final String name) {
        this.body.put("name", name);
    }

    @Given("the teacher's name is longer than 100 characters")
    public void theTeachersNameTooLong() {
        this.body.put("name", "a".repeat(101));
    }

    @Given("the teacher's name is not provided")
    public void theTeachersNameNotProvided() {
        // name field intentionally omitted from the request
    }

    @Given("the teacher's name is set to null")
    public void theTeachersNameIsNull() {
        this.body.putNull("name");
    }

    @Given("the teacher's name is left blank")
    public void theTeachersNameIsBlank() {
        this.body.put("name", "");
    }

    @Given("the teacher's name contains only whitespace")
    public void theTeachersNameContainsOnlyWhitespace() {
        this.body.put("name", "   ");
    }

    @And("the teacher's surname is {string}")
    public void theTeachersSurnameIs(final String surname) {
        this.body.put("surname", surname);
    }

    @And("the teacher's surname is longer than 100 characters")
    public void theTeachersSurnameTooLong() {
        this.body.put("surname", "b".repeat(101));
    }

    @And("the teacher's surname is not provided")
    public void theTeachersSurnameNotProvided() {
        // surname field intentionally omitted from the request
    }

    @And("the teacher's surname is set to null")
    public void theTeachersSurnameIsNull() {
        this.body.putNull("surname");
    }

    @And("the teacher's surname is left blank")
    public void theTeachersSurnameIsBlank() {
        this.body.put("surname", "");
    }

    @And("the teacher's surname contains only whitespace")
    public void theTeachersSurnameContainsOnlyWhitespace() {
        this.body.put("surname", "   ");
    }

    @And("the teacher's e-mail address is {string}")
    public void theTeachersEMailAddressIs(final String mail) {
        this.body.put("mail", mail);
    }

    @And("the teacher's e-mail address is longer than 200 characters")
    public void theTeachersEMailAddressTooLong() {
        this.body.put("mail", "c".repeat(201) + "@example.com");
    }

    @And("the teacher's e-mail address is not provided")
    public void theTeachersEMailAddressNotProvided() {
        // mail field intentionally omitted from the request
    }

    @And("the teacher's e-mail address is set to null")
    public void theTeachersEMailAddressIsNull() {
        this.body.putNull("mail");
    }

    @And("the teacher's e-mail address is left blank")
    public void theTeachersEMailAddressIsBlank() {
        this.body.put("mail", "");
    }

    @And("the teacher's e-mail address contains only whitespace")
    public void theTeachersEMailAddressContainsOnlyWhitespace() {
        this.body.put("mail", "   ");
    }

    @And("the teacher teaches at school {string}")
    public void theTeacherTeachesAtSchool(final String schoolName) {
        this.body.put("school", this.schoolLocation(schoolName));
    }

    @And("the teacher is already registered")
    public void theTeacherIsAlreadyRegistered() {
        //@formatter:off
        final Person person = Person.builder().id(UUID.randomUUID())
                                    .name(this.body.get("name").asString())
                                    .surname(this.body.get("surname").asString())
                                    .mail(this.body.get("mail").asString())
                                    .build();
        final Teacher teacher = Teacher.builder()
                                       .person(person)
                                       .school(this.school(this.body.get("school").asString()))
                                       .build();
        //@formatter:on
        TestSecurityContext.runAuthenticated(() -> this.teacherRepository.save(teacher));
    }

    private School school(final String location) {
        final UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.schoolRepository.findById(id).orElseThrow();
    }

    private String schoolLocation(final String name) {
        final String key = "school:" + name;
        if(this.scenarioContext.getLocation(key) == null) {
            final School saved = TestSecurityContext.runAuthenticated(
                    () -> this.schoolRepository.save(EntityFixtures.newSchool(name)));
            this.scenarioContext.putLocation(key, "/v0/schools/" + saved.getId());
        }
        return this.scenarioContext.getLocation(key);
    }

    @When("I click on \"Create teacher account\"")
    public void createTeacherAccount()
            throws Exception {
        this.performWithBody(post("/v0/teachers"), this.body);
    }

    @Then("I receive a confirmation that the teacher account has been successfully created")
    public void teacherAccountCreatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

}
