package edu.lyra.members.api.cucumber.classroom;

import java.util.UUID;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.cucumber.AbstractResourceFeatures;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClassroomCreationFeatures
        extends AbstractResourceFeatures {

    private static final MediaType URI_LIST = MediaType.parseMediaType("text/uri-list");

    @Autowired
    private ClassroomRepository classroomRepository;

    @Before(order = 0)
    public void cleanClassrooms() {
        this.classroomRepository.deleteAll();
    }

    @Given("teacher {string} has been added to the classroom")
    public void teacherHasBeenAddedToClassroom(final String teacherName)
            throws Exception {
        this.performAddTeacher(teacherName, adminJwtProcessor()).andExpect(status().isNoContent());
    }

    @When("I add teacher {string} to the classroom")
    public void addTeacherToClassroom(final String teacherName)
            throws Exception {
        this.scenarioContext.setResultActions(this.performAddTeacher(teacherName));
    }

    @When("I add teacher {string} to a classroom that does not exist")
    public void addTeacherToNonExistentClassroom(final String teacherName)
            throws Exception {
        //@formatter:off
        this.perform(post(replaceLastSegment(this.classroomLocation()) + "/teachers")
                             .contentType(URI_LIST)
                             .content(this.scenarioContext.getLocation("teacher:" + teacherName)));
        //@formatter:on
    }

    private static String replaceLastSegment(final String location) {
        return location.substring(0, location.lastIndexOf('/') + 1) + UUID.randomUUID();
    }

    @When("I create a classroom for course {int} group {string} at school {string} with tutor {string}")
    public void createClassroomWithTutor(
            final int course,
            final String group,
            final String schoolName,
            final String teacherName
    )
            throws Exception {
        //@formatter:off
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("course", course);
        body.put("group", group);
        body.put("school", this.scenarioContext.getLocation("school:" + schoolName));
        body.put("tutor", this.scenarioContext.getLocation("teacher:" + teacherName));
        this.performWithBody(post("/v0/classrooms"), body);
        //@formatter:on
    }

    private ResultActions performAddTeacher(final String teacherName, final RequestPostProcessor jwtProcessor)
            throws Exception {
        //@formatter:off
        return this.mvc.perform(post(this.classroomLocation() + "/teachers")
                                        .with(jwtProcessor)
                                        .contentType(URI_LIST)
                                        .content(this.scenarioContext.getLocation("teacher:" + teacherName)));
        //@formatter:on
    }

    @When("I set teacher {string} as the classroom's tutor")
    public void setTutor(final String teacherName)
            throws Exception {
        this.scenarioContext.setResultActions(this.performSetTutor(teacherName));
    }

    @When("I request the classroom's teachers")
    public void requestClassroomTeachers()
            throws Exception {
        this.perform(get(this.classroomLocation() + "/teachers"));
    }

    @When("I request the classroom's tutor")
    public void requestClassroomTutor()
            throws Exception {
        this.perform(get(this.classroomLocation() + "/tutor"));
    }

    @Then("I receive a confirmation that the teacher has been successfully added to the classroom")
    public void teacherAddedToClassroomOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

    @Then("I receive a confirmation that the tutor has been successfully set")
    public void tutorSetOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

    @Then("the classroom's teachers include {string}")
    public void classroomTeachersInclude(final String teacherName)
            throws Exception {
        //@formatter:off
        final MockHttpServletResponse response = this.scenarioContext.getResultActions()
                .andExpect(status().isOk()).andReturn().getResponse();
        final String expectedLocation = this.scenarioContext.getLocation("teacher:" + teacherName);
        final JsonNode teachers = OBJECT_MAPPER.readTree(response.getContentAsString())
                                               .path("_embedded")
                                               .path("teachers");
        boolean found = false;
        for(final JsonNode teacher : teachers) {
            final String selfLink = teacher.path("_links")
                                           .path("self")
                                           .path("href")
                                           .asString();
            if(selfLink.endsWith(expectedLocation)) {
                found = true;
                break;
            }
        }
        //@formatter:on
        assertTrue(found, "Expected classroom's teachers to include %s".formatted(teacherName));
    }

    @Then("the classroom's tutor is {string}")
    public void classroomTutorIs(final String teacherName)
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
        final String expectedLocation = this.scenarioContext.getLocation("teacher:" + teacherName);
        assertTrue(selfLink.endsWith(expectedLocation),
                   "Expected tutor link %s to match %s".formatted(selfLink, expectedLocation));
    }

    @Then("I receive an error because the teacher does not belong to the classroom's school")
    public void receiveSchoolMismatchError()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isUnprocessableContent());
    }

    @When("I create a classroom for course {int} group {string} at school {string}")
    public void createClassroom(final int course, final String group, final String schoolName)
            throws Exception {
        //@formatter:off
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("course", course);
        body.put("group", group);
        body.put("school", this.scenarioContext.getLocation("school:" + schoolName));
        this.performWithBody(post("/v0/classrooms"), body);
        //@formatter:on
    }

    @Then("I receive a confirmation that the classroom has been successfully created")
    public void classroomCreatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isCreated());
    }

    private String classroomLocation() {
        return this.scenarioContext.getLocation("classroom");
    }

    static RequestPostProcessor adminJwtProcessor() {
        //@formatter:off
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                    .authorities(new SimpleGrantedAuthority("SCOPE_classrooms.update"),
                                 new SimpleGrantedAuthority("ROLE_admin"));
        //@formatter:on
    }

    @Given("teacher {string} has been set as the classroom's tutor")
    public void teacherHasBeenSetAsTutor(final String teacherName)
            throws Exception {
        this.performSetTutor(teacherName, adminJwtProcessor()).andExpect(status().isNoContent());
    }

    @Then("I receive an error because the classroom already exists")
    public void receiveClassroomAlreadyExistsError()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isConflict());
    }

    private ResultActions performSetTutor(final String teacherName, final RequestPostProcessor jwtProcessor)
            throws Exception {
        //@formatter:off
        return this.mvc.perform(put(this.classroomLocation() + "/tutor")
                                        .with(jwtProcessor)
                                        .contentType(URI_LIST)
                                        .content(this.scenarioContext.getLocation("teacher:" + teacherName)));
        //@formatter:on
    }

    private ResultActions performAddTeacher(final String teacherName)
            throws Exception {
        return this.performAddTeacher(teacherName, this.scenarioContext.getJwtProcessor());
    }

    private ResultActions performSetTutor(final String teacherName)
            throws Exception {
        return this.performSetTutor(teacherName, this.scenarioContext.getJwtProcessor());
    }

    @When("I update the classroom's course to {int} and group to {string}")
    public void updateClassroom(final int course, final String group)
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("course", course);
        body.put("group", group);
        this.performWithBody(patch(this.classroomLocation()), body);
    }

    @When("I update a classroom that does not exist")
    public void updateNonExistentClassroom()
            throws Exception {
        final ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("course", 4);
        body.put("group", "B");
        this.performWithBody(patch("/v0/classrooms/" + UUID.randomUUID()), body);
    }

    @Then("I receive a confirmation that the classroom has been successfully updated")
    public void classroomUpdatedOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

    @When("I enroll kid {string} {string} in the classroom")
    public void enrollKidInClassroom(final String name, final String surname)
            throws Exception {
        //@formatter:off
        this.perform(post(this.classroomLocation() + "/kids")
                             .contentType(URI_LIST)
                             .content(this.scenarioContext.getLocation("kid:" + name + " " + surname)));
        //@formatter:on
    }

    @Then("I receive a confirmation that the kid has been successfully enrolled")
    public void kidEnrolledOk()
            throws Exception {
        this.scenarioContext.getResultActions().andExpect(status().isNoContent());
    }

}
