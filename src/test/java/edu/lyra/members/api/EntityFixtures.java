package edu.lyra.members.api;

import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.ClassroomsRepository;
import edu.lyra.members.api.repositories.jpa.ContactInfo;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.SchoolsRepository;
import edu.lyra.members.api.repositories.jpa.Teacher;
import edu.lyra.members.api.repositories.jpa.TeachersRepository;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class EntityFixtures {

    @Autowired
    private SchoolsRepository schoolsRepository;

    @Autowired
    private ClassroomsRepository classroomsRepository;

    @Autowired
    private TeachersRepository teachersRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    @Given("a school named {string} exists")
    public void aSchoolNamedExists(final String name) {
        final School school = new School();
        ReflectionTestUtils.setField(school, "name", name);
        final School saved = TestSecurityContext.runAuthenticated(() -> this.schoolsRepository.save(school));
        this.scenarioContext.putLocation("school:" + name, "/v0/schools/" + saved.getId());
    }

    @Given("a classroom for course {int} group {string} exists at school {string}")
    public void aClassroomExistsAtSchool(final int course, final String group, final String schoolName) {
        final Classroom classroom = new Classroom();
        ReflectionTestUtils.setField(classroom, "course", course);
        ReflectionTestUtils.setField(classroom, "group", group);
        ReflectionTestUtils.setField(classroom, "school", this.school(schoolName));
        final Classroom saved = TestSecurityContext.runAuthenticated(() -> this.classroomsRepository.save(classroom));
        this.scenarioContext.putLocation("classroom", "/v0/classrooms/" + saved.getId());
    }

    @Given("a teacher named {string} {string} exists at school {string} with e-mail {string}")
    public void aTeacherExistsAtSchool(
            final String name, final String surname, final String schoolName, final String mail
    ) {
        //@formatter:off
        final Teacher teacher = Teacher.builder()
                                       .id(UUID.randomUUID())
                                       .contactInfo(ContactInfo.builder().name(name).surname(surname).mail(mail).build())
                                       .school(this.school(schoolName))
                                       .build();
        //@formatter:on
        final Teacher saved = TestSecurityContext.runAuthenticated(() -> this.teachersRepository.save(teacher));
        this.scenarioContext.putLocation("teacher:" + name + " " + surname, "/v0/teachers/" + saved.getId());
    }

    private School school(final String name) {
        final String location = this.scenarioContext.getLocation("school:" + name);
        final UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.schoolsRepository.findById(id).orElseThrow();
    }

}
