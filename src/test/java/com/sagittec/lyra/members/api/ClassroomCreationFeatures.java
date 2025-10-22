package com.sagittec.lyra.members.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sagittec.lyra.members.api.repositories.Classroom;
import com.sagittec.lyra.members.api.repositories.ClassroomsRepository;
import com.sagittec.lyra.members.api.repositories.School;
import com.sagittec.lyra.members.api.repositories.SchoolsRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClassroomCreationFeatures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern REPEAT_PATTERN = Pattern.compile("^([a-zA-Z])\\((\\d+)\\)(.*)$");

    private final ObjectNode classroomJson = OBJECT_MAPPER.createObjectNode();

    private ResultActions resultActions;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClassroomsRepository classroomsRepository;

    @Autowired
    private SchoolsRepository schoolsRepository;

    @Before
    public void cleanJson() {
        this.classroomJson.removeAll();
        this.classroomsRepository.deleteAll();
        this.schoolsRepository.deleteAll();
    }

    @Given("the classroom course is {word}")
    public void classroomCourseIs(final String courseToken) {
        this.putMaybeNumber("course", courseToken);
    }

    @And("the classroom group is {string}")
    public void classroomGroupIs(final String groupToken) {
        this.putMaybe("group", groupToken);
    }

    @When("I click on \"Create classroom\"")
    public void createClassroom()
            throws Exception {
        final String content = OBJECT_MAPPER.writeValueAsString(this.classroomJson);
        this.resultActions = this.mvc.perform(post("/classrooms").contentType(APPLICATION_JSON).content(content));
    }

    @Then("I receive a confirmation that the classroom has been successfully created")
    public void classroomCreatedOk()
            throws Exception {
        this.resultActions.andExpect(status().isCreated());
    }

    @Then("I receive an error because the classroom data is invalid")
    public void classroomCreationBadRequest()
            throws Exception {
        this.resultActions.andExpect(status().isBadRequest());
    }

    @And("this classroom already exists")
    public void classroomAlreadyExists() {
        //@formatter:off
        final School school = this.schoolsRepository.save(School.builder().name("school").build());
        final Classroom classroomEntity = Classroom.builder()
                                             .course(this.classroomJson.get("course").asInt())
                                             .group(this.classroomJson.get("group").asText())
                                             .school(school)
                                             .build();
        //@formatter:on
        this.classroomJson.putPOJO("school", school);
        this.classroomsRepository.save(classroomEntity);
    }

    @Then("I receive an error because the classroom already exists")
    public void classroomAlreadyExistsConflict()
            throws Exception {
        this.resultActions.andExpect(status().isConflict());
    }

    private void putMaybe(String field, String token) {
        if(token == null) {
            this.classroomJson.putNull(field);
            return;
        }
        switch(token) {
            case "missing":
                return; // omit field
            case "null":
                this.classroomJson.putNull(field);
                return;
            case "empty":
                this.classroomJson.put(field, "");
                return;
            case "spaces":
                this.classroomJson.put(field, "   ");
                return;
            default:
                this.classroomJson.put(field, expand(token));
        }
    }

    private void putMaybeNumber(String field, String token) {
        if(token == null) {
            this.classroomJson.putNull(field);
            return;
        }
        switch(token) {
            case "missing":
                return; // omit field
            case "null":
                this.classroomJson.putNull(field);
                return;
            default:
                try {
                    int value = Integer.parseInt(token);
                    this.classroomJson.put(field, value);
                } catch(NumberFormatException nfe) {
                    // put as-is to trigger validation error on server side
                    this.classroomJson.put(field, token);
                }
        }
    }

    private String expand(String token) {
        final Matcher m = REPEAT_PATTERN.matcher(token);
        if(m.matches()) {
            char         ch     = m.group(1).charAt(0);
            int          count  = Integer.parseInt(m.group(2));
            final String suffix = m.group(3) == null ? "" : m.group(3);
            return ("" + ch).repeat(Math.max(0, count)) + suffix;
        }
        return token;
    }

}
