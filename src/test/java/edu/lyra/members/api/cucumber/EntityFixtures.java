package edu.lyra.members.api.cucumber;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Autowired;

import static org.instancio.Select.field;

public class EntityFixtures {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    public static School newSchool(final String name) {
        //@formatter:off
        return Instancio.of(School.class)
                         .ignore(field(School.class, "id"))
                         .ignore(field(School.class, "classrooms"))
                         .ignore(field(School.class, "teachers"))
                         .ignore(field(Auditable.class, "version"))
                         .ignore(field(Auditable.class, "createdDate"))
                         .ignore(field(Auditable.class, "createdBy"))
                         .ignore(field(Auditable.class, "lastModifiedDate"))
                         .ignore(field(Auditable.class, "updatedBy"))
                         .set(field(School.class, "name"), name)
                         .create();
        //@formatter:on
    }

    @Before(order = 0)
    public void cleanSchools() {
        this.classroomRepository.deleteAll();
        this.teacherRepository.deleteAll();
        this.schoolRepository.deleteAll();
    }

    /**
     * Runs after every other scenario-setup {@code @Before} hook (Cucumber's default order is 10000) since a
     * {@link Person} can only be deleted once nothing still references it via the shared-primary-key {@code parent}/
     * {@code teacher} role rows those hooks clear.
     */
    @Before(order = 20000)
    public void cleanPersons() {
        this.personRepository.deleteAll();
    }

    @Given("a school named {string} exists")
    public void aSchoolNamedExists(final String name) {
        final School saved = TestSecurityContext.runAuthenticated(() -> this.schoolRepository.save(newSchool(name)));
        this.scenarioContext.putLocation("school:" + name, "/v0/schools/" + saved.getId());
    }

    @Given("the following schools exist:")
    public void theFollowingSchoolsExist(final DataTable table) {
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for(final Map<String, String> row : rows) {
            this.aSchoolNamedExists(row.get("name"));
        }
    }

    @Given("a classroom for course {int} group {string} exists at school {string}")
    public void aClassroomExistsAtSchool(final int course, final String group, final String schoolName) {
        //@formatter:off
        final Classroom classroom = Instancio.of(Classroom.class).ignore(field(Classroom.class, "id"))
                                             .ignore(field(Classroom.class, "tutor"))
                                             .ignore(field(Classroom.class, "teachers"))
                                             .ignore(field(Classroom.class, "kids"))
                                             .ignore(field(Auditable.class, "version"))
                                             .ignore(field(Auditable.class, "createdDate"))
                                             .ignore(field(Auditable.class, "createdBy"))
                                             .ignore(field(Auditable.class, "lastModifiedDate"))
                                             .ignore(field(Auditable.class, "updatedBy"))
                                             .set(field(Classroom.class, "course"), course)
                                             .set(field(Classroom.class, "group"), group)
                                             .set(field(Classroom.class, "school"), this.school(schoolName)).create();
        //@formatter:on
        final Classroom saved = TestSecurityContext.runAuthenticated(() -> this.classroomRepository.save(classroom));
        this.scenarioContext.putLocation("classroom", "/v0/classrooms/" + saved.getId());
        this.scenarioContext.putLocation("classroom:" + course + " " + group, "/v0/classrooms/" + saved.getId());
    }

    @Given("the following classrooms exist at school {string}:")
    public void theFollowingClassroomsExistAtSchool(final String schoolName, final DataTable table) {
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for(final Map<String, String> row : rows) {
            this.aClassroomExistsAtSchool(Integer.parseInt(row.get("course")), row.get("group"), schoolName);
        }
    }

    private School school(final String name) {
        final String location = this.scenarioContext.getLocation("school:" + name);
        final UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return this.schoolRepository.findById(id).orElseThrow();
    }

    @Given("a teacher named {string} {string} exists at school {string} with e-mail {string}")
    public void aTeacherExistsAtSchool(
            final String name,
            final String surname,
            final String schoolName,
            final String mail
    ) {
        //@formatter:off
        final Person person = Person.builder().id(UUID.randomUUID()).name(name).surname(surname).mail(mail).build();
        final Teacher teacher = Teacher.builder()
                                       .person(person)
                                       .school(this.school(schoolName))
                                       .build();
        //@formatter:on
        final Teacher saved = TestSecurityContext.runAuthenticated(() -> this.teacherRepository.save(teacher));
        this.scenarioContext.putLocation("teacher:" + name + " " + surname, "/v0/teachers/" + saved.getId());
    }

    @Given("the following teachers exist at school {string}:")
    public void theFollowingTeachersExistAtSchool(final String schoolName, final DataTable table) {
        final List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for(final Map<String, String> row : rows) {
            this.aTeacherExistsAtSchool(row.get("name"), row.get("surname"), schoolName, row.get("mail"));
        }
    }

    @Given("a person named {string} {string} exists with e-mail {string}")
    public void aPersonExists(final String name, final String surname, final String mail) {
        final Person person = Person.builder().id(UUID.randomUUID()).name(name).surname(surname).mail(mail).build();
        final Person saved  = TestSecurityContext.runAuthenticated(() -> this.personRepository.save(person));
        this.scenarioContext.putLocation("person:" + name + " " + surname, "/v0/persons/" + saved.getId());
    }

}
