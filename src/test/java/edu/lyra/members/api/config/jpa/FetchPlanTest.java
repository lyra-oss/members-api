package edu.lyra.members.api.config.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRole;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the number of SQL statements the paged read paths issue.
 *
 * <p>These assertions are the guard for the fetch plan of the domain model. Every {@code @ManyToOne}/{@code @OneToOne}
 * is lazy, and the two role listings join-fetch the {@code Person} they render, so a page costs one query plus its
 * count regardless of how many distinct parents, classrooms or schools the page spans. Making an association eager
 * again, or dropping an {@code @EntityGraph}, turns a page back into one query per row: the counts below move and
 * these tests fail.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@Import(FixedAuditorConfiguration.class)
class FetchPlanTest {

    private static final int PAGE_SIZE = 20;

    private static final Pageable FIRST_PAGE = PageRequest.of(0, PAGE_SIZE);

    /**
     * One query for the page plus one for its total. Spring Data only skips the count query when the page is not
     * full, and every page built here is exactly full.
     */
    private static final long QUERY_PLUS_COUNT = 2L;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager em;

    @Autowired
    private KidRepository kidRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private UUID prolificParentId;

    @BeforeEach
    void seedOneDistinctGraphPerKid() {
        Classroom lastClassroom = null;
        for(int i = 0; i < PAGE_SIZE; i++) {
            final School school = new School();
            school.setName("school-" + i);
            this.entityManager.persist(school);

            final Teacher tutor = Teacher.builder().person(aPerson("tutor-" + i)).school(school).build();
            this.entityManager.persist(tutor);

            final Classroom classroom = new Classroom();
            classroom.setCourse(1);
            classroom.setGroup("A");
            classroom.setSchool(school);
            classroom.setTutor(tutor);
            this.entityManager.persist(classroom);

            final Parent parent = Parent.builder().person(aPerson("parent-" + i)).build();
            this.entityManager.persist(parent);

            this.entityManager.persist(aKid("kid-" + i, i, parent, classroom));
            lastClassroom = classroom;
        }

        // One parent with a full page of kids, so the paged lookup below is a full page and keeps its count query.
        final Parent prolific = Parent.builder().person(aPerson("prolific")).build();
        this.entityManager.persist(prolific);
        for(int i = 0; i < PAGE_SIZE; i++) {
            this.entityManager.persist(aKid("sibling-" + i, i, prolific, lastClassroom));
        }
        this.prolificParentId = prolific.getId();

        this.entityManager.flush();
        this.entityManager.clear();
    }

    @Test
    void listingKidsCostsOneQueryAndItsCountHoweverManyParentsThePageSpans() {
        final Statistics statistics = this.clearedStatistics();

        final List<Kid> kids = this.kidRepository.findAll(FIRST_PAGE).getContent();

        assertEquals(PAGE_SIZE, kids.size());
        assertEquals(QUERY_PLUS_COUNT, statistics.getPrepareStatementCount(),
                     "a page of kids must not trigger a query per distinct parent or classroom");
        assertEquals(PAGE_SIZE, statistics.getEntityLoadCount(),
                     "only the kids themselves are rendered, so nothing else may be loaded");
    }

    @Test
    void listingOneParentsKidsCostsOneQueryAndItsCount() {
        final Statistics statistics = this.clearedStatistics();

        final List<Kid> kids =
                this.kidRepository.findByParentIdOrderByNameAsc(this.prolificParentId, FIRST_PAGE).getContent();

        assertEquals(PAGE_SIZE, kids.size());
        assertEquals(QUERY_PLUS_COUNT, statistics.getPrepareStatementCount(),
                     "a parent's kids must not trigger a query per kid");
        assertEquals(PAGE_SIZE, statistics.getEntityLoadCount(),
                     "only the kids themselves are rendered, so their parent and classroom must stay unloaded");
    }

    @Test
    void listingParentsFetchesTheirPersonInTheSameQuery() {
        final Statistics statistics = this.clearedStatistics();

        final List<Parent> parents = this.parentRepository.findAll(FIRST_PAGE).getContent();
        readTheRenderedIdentityFieldsOf(parents);

        assertEquals(PAGE_SIZE, parents.size());
        assertEquals(QUERY_PLUS_COUNT, statistics.getPrepareStatementCount(),
                     "ParentRepository.findAll must join-fetch person; a lazy person costs one query per parent");
    }

    @Test
    void listingTeachersFetchesTheirPersonInTheSameQuery() {
        final Statistics statistics = this.clearedStatistics();

        final List<Teacher> teachers = this.teacherRepository.findAll(FIRST_PAGE).getContent();
        readTheRenderedIdentityFieldsOf(teachers);

        assertEquals(PAGE_SIZE, teachers.size());
        assertEquals(QUERY_PLUS_COUNT, statistics.getPrepareStatementCount(),
                     "TeacherRepository.findAll must join-fetch person; a lazy person costs one query per teacher");
    }

    @Test
    void countingAParentsKidsLoadsNoKids() {
        final Statistics statistics = this.clearedStatistics();

        final long kids = this.kidRepository.countByParentId(this.prolificParentId);

        assertEquals(PAGE_SIZE, kids);
        assertEquals(1L, statistics.getPrepareStatementCount(), "counting must be a single statement");
        assertEquals(0L, statistics.getEntityLoadCount(),
                     "the delete guards must count in the database, never load the rows they are counting");
    }

    /**
     * Reads exactly what the {@code *Mapper} reads when it renders a role, so a lazily loaded {@code Person} would
     * show up as an extra statement here just as it would in production.
     *
     * @param roles the roles to render
     */
    private static void readTheRenderedIdentityFieldsOf(final Iterable<? extends PersonRole> roles) {
        roles.forEach(role -> {
            role.getName();
            role.getSurname();
            role.getMail();
        });
    }

    private Statistics clearedStatistics() {
        final Statistics statistics = this.em.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();
        return statistics;
    }

    private static Person aPerson(final String slug) {
        return Person.builder().id(UUID.randomUUID()).name(slug).surname(slug).mail(slug + "@example.com").build();
    }

    private static Kid aKid(final String name, final int offset, final Parent parent, final Classroom classroom) {
        final Kid kid = new Kid();
        kid.setName(name);
        kid.setSurname(name);
        kid.setBirthdate(LocalDate.of(2015, 1, 1).plusDays(offset));
        kid.setParent(parent);
        kid.setClassroom(classroom);
        return kid;
    }

}
