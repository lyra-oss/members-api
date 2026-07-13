package edu.lyra.members.api;

import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.ContactInfo;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.SchoolsRepository;
import edu.lyra.members.api.repositories.jpa.Teacher;
import edu.lyra.members.api.repositories.jpa.TeachersRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TeacherCreationFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectNode teacherJson = OBJECT_MAPPER.createObjectNode();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SchoolsRepository schoolsRepository;

    @Autowired
    private TeachersRepository teachersRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    @Before
    public void cleanJson() {
        this.teacherJson.removeAll();
        this.teachersRepository.deleteAll();
    }

    @Given("the teacher's name is {string}")
    public void theTeachersNameIs(final String name) {
        this.teacherJson.put("name", name);
    }

    @Given("the teacher's name is longer than 100 characters")
    public void theTeachersNameTooLong() {
        this.teacherJson.put("name", "a".repeat(101));
    }

    @Given("the teacher's name is not provided")
    public void theTeachersNameNotProvided() {
        // name field intentionally omitted from the request
    }

    @Given("the teacher's name is set to null")
    public void theTeachersNameIsNull() {
        this.teacherJson.putNull("name");
    }

    @Given("the teacher's name is left blank")
    public void theTeachersNameIsBlank() {
        this.teacherJson.put("name", "");
    }

    @Given("the teacher's name contains only whitespace")
    public void theTeachersNameContainsOnlyWhitespace() {
        this.teacherJson.put("name", "   ");
    }

    @And("the teacher's surname is {string}")
    public void theTeachersSurnameIs(final String surname) {
        this.teacherJson.put("surname", surname);
    }

    @And("the teacher's surname is longer than 100 characters")
    public void theTeachersSurnameTooLong() {
        this.teacherJson.put("surname", "b".repeat(101));
    }

    @And("the teacher's surname is not provided")
    public void theTeachersSurnameNotProvided() {
        // surname field intentionally omitted from the request
    }

    @And("the teacher's surname is set to null")
    public void theTeachersSurnameIsNull() {
        this.teacherJson.putNull("surname");
    }

    @And("the teacher's surname is left blank")
    public void theTeachersSurnameIsBlank() {
        this.teacherJson.put("surname", "");
    }

    @And("the teacher's surname contains only whitespace")
    public void theTeachersSurnameContainsOnlyWhitespace() {
        this.teacherJson.put("surname", "   ");
    }

    @And("the teacher's e-mail address is {string}")
    public void theTeachersEMailAddressIs(final String mail) {
        this.teacherJson.put("mail", mail);
    }

    @And("the teacher's e-mail address is longer than 200 characters")
    public void theTeachersEMailAddressTooLong() {
        this.teacherJson.put("mail", "c".repeat(201) + "@example.com");
    }

    @And("the teacher's e-mail address is not provided")
    public void theTeachersEMailAddressNotProvided() {
        // mail field intentionally omitted from the request
    }

    @And("the teacher's e-mail address is set to null")
    public void theTeachersEMailAddressIsNull() {
        this.teacherJson.putNull("mail");
    }

    @And("the teacher's e-mail address is left blank")
    public void theTeachersEMailAddressIsBlank() {
        this.teacherJson.put("mail", "");
    }

    @And("the teacher's e-mail address contains only whitespace")
    public void theTeachersEMailAddressContainsOnlyWhitespace() {
        this.teacherJson.put("mail", "   ");
    }

    @And("the teacher teaches at school {string}")
    public void theTeacherTeachesAtSchool(final String schoolName) {
        this.teacherJson.put("school", this.schoolLocation(schoolName));
    }

    @And("the teacher is already registered")
    public void theTeacherIsAlreadyRegistered() {
        //@formatter:off
        final Teacher teacher = Teacher.builder()
                                       .id(UUID.randomUUID())
                                       .contactInfo(ContactInfo.builder()
                                                               .name(this.teacherJson.get("name").asString())
                                                               .surname(this.teacherJson.get("surname").asString())
                                                               .mail(this.teacherJson.get("mail").asString())
                                                               .build())
                                       .school(this.school(this.teacherJson.get("school").asString()))
                                       .build();
        //@formatter:on
        TestSecurityContext.runAuthenticated(() -> this.teachersRepository.save(teacher));
    }

    private String schoolLocation(final String name) {
        final String key = "school:" + name;
        if(this.scenarioContext.getLocation(key) == null) {
            final School saved = TestSecurityContext.runAuthenticated(
                    () -> this.schoolsRepository.save(EntityFixtures.newSchool(name)));
            this.scenarioContext.putLocation(key, "/v0/schools/" + saved.getId());
        }
        return this.scenarioContext.getLocation(key);
    }

    private School school(final String location) {
        final UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.schoolsRepository.findById(id).orElseThrow();
    }

    @When("I click on \"Create teacher account\"")
    public void createTeacherAccount()
            throws Exception {
        final String content = OBJECT_MAPPER.writeValueAsString(this.teacherJson);
        this.scenarioContext.setResultActions(this.mvc.perform(
                post("/v0/teachers").with(this.scenarioContext.getJwtProcessor()).contentType(APPLICATION_JSON)
                                    .content(content)));
    }

    @Then("I receive a confirmation that the teacher account has been successfully created")
    public void teacherAccountCreatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

}
