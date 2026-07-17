package edu.lyra.members.api.cucumber.person;

import java.time.LocalDate;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import edu.lyra.members.api.cucumber.TestSecurityContext;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.node.ObjectNode;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PersonRoleFeatures
        extends AbstractResourceFeatures {

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private KidRepository kidRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @When("I make {string} {string} a parent")
    public void makeParent(final String name, final String surname)
            throws Exception {
        this.perform(put(this.personLocation(name, surname) + "/parent"));
    }

    private String personLocation(final String name, final String surname) {
        return this.scenarioContext.getLocation("person:" + name + " " + surname);
    }

    @When("I make {string} {string} a teacher at school {string}")
    public void makeTeacher(final String name, final String surname, final String schoolName)
            throws Exception {
        this.performWithBody(put(this.personLocation(name, surname) + "/teacher"), this.schoolBody(schoolName));
    }

    private ObjectNode schoolBody(final String schoolName) {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("school", this.scenarioContext.getLocation("school:" + schoolName));
        return body;
    }

    @When("I try to make {string} {string} a teacher without a school")
    public void makeTeacherWithoutSchool(final String name, final String surname)
            throws Exception {
        this.performWithBody(put(this.personLocation(name, surname) + "/teacher"), OBJECT_MAPPER.createObjectNode());
    }

    @When("I try to make a person that does not exist a parent")
    public void makeNonExistentPersonAParent()
            throws Exception {
        this.perform(put("/v0/persons/" + UUID.randomUUID() + "/parent"));
    }

    @When("I revoke {string} {string}'s parent role")
    public void revokeParent(final String name, final String surname)
            throws Exception {
        this.perform(delete(this.personLocation(name, surname) + "/parent"));
    }

    @When("I revoke {string} {string}'s teacher role")
    public void revokeTeacher(final String name, final String surname)
            throws Exception {
        this.perform(delete(this.personLocation(name, surname) + "/teacher"));
    }

    @Given("{string} {string} has been made a parent")
    public void hasBeenMadeParent(final String name, final String surname)
            throws Exception {
        //@formatter:off
        this.mvc.perform(put(this.personLocation(name, surname) + "/parent")
                                 .with(adminJwtProcessor("parents.create")))
                .andExpect(status().isNoContent());
        //@formatter:on
    }

    static RequestPostProcessor adminJwtProcessor(final String scope) {
        //@formatter:off
        return SecurityMockMvcRequestPostProcessors.jwt()
                    .jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                    .authorities(new SimpleGrantedAuthority("SCOPE_" + scope),
                                 new SimpleGrantedAuthority("ROLE_admin"));
        //@formatter:on
    }

    @Given("{string} {string} has been made a teacher at school {string}")
    public void hasBeenMadeTeacher(final String name, final String surname, final String schoolName)
            throws Exception {
        //@formatter:off
        this.mvc.perform(put(this.personLocation(name, surname) + "/teacher")
                                 .with(adminJwtProcessor("teachers.create"))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content(OBJECT_MAPPER.writeValueAsString(this.schoolBody(schoolName))))
                .andExpect(status().isNoContent());
        //@formatter:on
    }

    @Given("{string} {string} has a kid named {string} {string} born on {string}")
    public void hasAKid(
            final String name,
            final String surname,
            final String kidName,
            final String kidSurname,
            final String birthdate
    ) {
        final Parent parent = this.parentRepository.findById(this.personId(name, surname)).orElseThrow();
        //@formatter:off
        final Kid kid = Instancio.of(Kid.class)
                                 .ignore(field(Kid.class, "id"))
                                 .ignore(field(Kid.class, "classroom"))
                                 .ignore(field(Auditable.class, "version"))
                                 .ignore(field(Auditable.class, "createdDate"))
                                 .ignore(field(Auditable.class, "createdBy"))
                                 .ignore(field(Auditable.class, "lastModifiedDate"))
                                 .ignore(field(Auditable.class, "updatedBy"))
                                 .set(field(Kid.class, "name"), kidName)
                                 .set(field(Kid.class, "surname"), kidSurname)
                                 .set(field(Kid.class, "birthdate"), LocalDate.parse(birthdate))
                                 .set(field(Kid.class, "parent"), parent)
                                 .create();
        //@formatter:on
        TestSecurityContext.runAuthenticated(() -> this.kidRepository.save(kid));
    }

    private UUID personId(final String name, final String surname) {
        final String location = this.personLocation(name, surname);
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    @Given("{string} {string} tutors a classroom")
    public void tutorsAClassroom(final String name, final String surname) {
        final Teacher teacher = this.teacherRepository.findById(this.personId(name, surname)).orElseThrow();
        //@formatter:off
        final Classroom classroom = Instancio.of(Classroom.class)
                                             .ignore(field(Classroom.class, "id"))
                                             .ignore(field(Classroom.class, "teachers"))
                                             .ignore(field(Classroom.class, "kids"))
                                             .ignore(field(Auditable.class, "version"))
                                             .ignore(field(Auditable.class, "createdDate"))
                                             .ignore(field(Auditable.class, "createdBy"))
                                             .ignore(field(Auditable.class, "lastModifiedDate"))
                                             .ignore(field(Auditable.class, "updatedBy"))
                                             .set(field(Classroom.class, "course"), 3)
                                             .set(field(Classroom.class, "group"), "A")
                                             .set(field(Classroom.class, "school"), teacher.getSchool())
                                             .set(field(Classroom.class, "tutor"), teacher)
                                             .create();
        //@formatter:on
        TestSecurityContext.runAuthenticated(() -> this.classroomRepository.save(classroom));
    }

    @Then("I receive a confirmation that the role has been successfully granted")
    public void roleGrantedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

    @Then("I receive a confirmation that the role has been successfully revoked")
    public void roleRevokedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

    @Then("{string} {string} holds the parent role")
    public void holdsParentRole(final String name, final String surname) {
        assertTrue(this.parentRepository.existsById(this.personId(name, surname)),
                   "Expected %s %s to hold the parent role".formatted(name, surname));
    }

    @Then("{string} {string} does not hold the parent role")
    public void doesNotHoldParentRole(final String name, final String surname) {
        assertFalse(this.parentRepository.existsById(this.personId(name, surname)),
                    "Expected %s %s not to hold the parent role".formatted(name, surname));
    }

    @Then("{string} {string} holds the teacher role")
    public void holdsTeacherRole(final String name, final String surname) {
        assertTrue(this.teacherRepository.existsById(this.personId(name, surname)),
                   "Expected %s %s to hold the teacher role".formatted(name, surname));
    }

    @Then("{string} {string} does not hold the teacher role")
    public void doesNotHoldTeacherRole(final String name, final String surname) {
        assertFalse(this.teacherRepository.existsById(this.personId(name, surname)),
                    "Expected %s %s not to hold the teacher role".formatted(name, surname));
    }

}
