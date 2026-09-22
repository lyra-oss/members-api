package edu.lyra.members.api.config.jpa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static java.util.Comparator.comparing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that every paged repository read slices its result set correctly and at a constant cost.
 *
 * <p>The Cucumber suite already pages the five top-level listings over HTTP. What it never pages are the scoped reads
 * behind the association endpoints, and those are the ones that can go wrong quietly:
 *
 * <ul>
 *     <li>{@code findByClassroomTaughtOrTutoredBy} and {@code findByClassroomId} join a to-many and de-duplicate with
 *     {@code distinct}, each against a hand-written count query. A page must hold distinct entities, and the total
 *     must count entities rather than joined rows;</li>
 *     <li>three of these reads carry an {@code @EntityGraph}, and a fetch plan that multiplied rows would break
 *     {@code LIMIT} and the count with it.</li>
 * </ul>
 *
 * <p>Each assertion therefore covers both halves of the contract: the pages partition the result set exactly once,
 * and paging costs one query plus its count however many rows the joins produce underneath. Spring Data skips the
 * count when a page comes back short, so a full page costs two statements and a short final page costs one.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@DataJpaTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true" })
@Import(FixedAuditorConfiguration.class)
class PaginationTest {

    private static final int PAGE_SIZE = 2;

    private static final int KIDS = 5;

    private static final int STAFF_TEACHERS = 3;

    private static final long QUERY_PLUS_COUNT = 2L;

    private static final long QUERY_ONLY = 1L;

    /** The order every role listing promises: the rendered name, then surname, then the id that makes it total. */
    private static final Comparator<PersonRole> BY_RENDERED_NAME =
            comparing(PersonRole::getName).thenComparing(PersonRole::getSurname).thenComparing(PersonRole::getId);

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

    @Autowired
    private ClassroomRepository classroomRepository;

    private UUID schoolId;

    private UUID classroomId;

    private UUID tutorId;

    private UUID staffTeacherId;

    private UUID parentId;

    private List<UUID> kidIdsByName;

    private List<UUID> parentIds;

    private List<UUID> classroomStaffIds;

    private List<UUID> schoolTeacherIds;

    @BeforeEach
    void seed() {
        final School school = new School();
        school.setName("Gloria Fuertes");
        this.entityManager.persist(school);
        this.schoolId = school.getId();

        final Teacher tutor = Teacher.builder().person(aPerson("zz-tutor")).school(school).build();
        this.entityManager.persist(tutor);
        this.tutorId = tutor.getId();

        final Classroom classroom = new Classroom();
        classroom.setCourse(1);
        classroom.setGroup("A");
        classroom.setSchool(school);
        classroom.setTutor(tutor);

        // The tutor is deliberately not on the teaching staff: every kid then matches the tutor branch of
        // findByClassroomTaughtOrTutoredBy once per staff teacher, so the join multiplies rows STAFF_TEACHERS-fold
        // and only 'distinct' brings it back to one row per kid.
        // Named backwards so that seeding order is not name order: the listings are sorted by the person's name, so
        // a fixture that recorded insertion order would pass whether or not the query ordered anything.
        final List<Teacher> staffTeachers = new ArrayList<>();
        for(int i = 0; i < STAFF_TEACHERS; i++) {
            final Teacher staff =
                    Teacher.builder().person(aPerson("staff-" + (char) ('c' - i))).school(school).build();
            this.entityManager.persist(staff);
            classroom.getTeachers().add(staff);
            staffTeachers.add(staff);
            this.staffTeacherId = staff.getId();
        }
        this.classroomStaffIds = staffTeachers.stream().sorted(BY_RENDERED_NAME).map(Teacher::getId).toList();
        this.schoolTeacherIds = Stream.concat(Stream.of(tutor), staffTeachers.stream())
                                      .sorted(BY_RENDERED_NAME).map(Teacher::getId).toList();
        this.entityManager.persist(classroom);
        this.classroomId = classroom.getId();

        // Two further classrooms at the same school, so the school's classrooms are worth paging.
        for(int i = 0; i < 2; i++) {
            final Classroom other = new Classroom();
            other.setCourse(2 + i);
            other.setGroup("A");
            other.setSchool(school);
            this.entityManager.persist(other);
        }

        final Parent parent = Parent.builder().person(aPerson("parent")).build();
        this.entityManager.persist(parent);
        this.parentId = parent.getId();

        // Named so that seeding order is not name order: the listing is sorted by the person's name, so a fixture
        // that simply recorded insertion order would pass whether or not the query ordered anything.
        final List<Parent> parents = new ArrayList<>(List.of(parent));
        for(int i = 0; i < 2; i++) {
            final Parent other = Parent.builder().person(aPerson("aardvark-parent-" + i)).build();
            this.entityManager.persist(other);
            parents.add(other);
        }
        this.parentIds = parents.stream()
                                .sorted(comparing(Parent::getName).thenComparing(Parent::getSurname)
                                                                  .thenComparing(Parent::getId))
                                .map(Parent::getId).toList();

        // Names are seeded in ascending order so the list doubles as the expected order for the sorted query.
        this.kidIdsByName = new ArrayList<>();
        for(int i = 0; i < KIDS; i++) {
            final Kid kid = new Kid();
            kid.setName("kid-" + (char) ('a' + i));
            kid.setSurname("Cristóbal");
            kid.setBirthdate(LocalDate.of(2015, 1, 1).plusDays(i));
            kid.setParent(parent);
            kid.setClassroom(classroom);
            this.entityManager.persist(kid);
            this.kidIdsByName.add(kid.getId());
        }

        this.entityManager.flush();
        this.entityManager.clear();
    }

    @Test
    void aParentsKidsArePagedInNameOrderAtAConstantCost() {
        assertPaging("a parent's kids", this.kidIdsByName,
                     page -> this.kidRepository.findByParentIdOrderByNameAsc(this.parentId, PageRequest.of(page,
                                                                                                          PAGE_SIZE)),
                     Kid::getId);
    }

    @Test
    void aTutorsKidsArePagedWithoutTheJoinLeakingIntoThePagesOrTheTotal() {
        assertPaging("the kids a tutor may see", this.kidIdsByName,
                     page -> this.kidRepository.findByClassroomTaughtOrTutoredBy(this.tutorId,
                                                                                 PageRequest.of(page, PAGE_SIZE)),
                     Kid::getId);
    }

    @Test
    void aStaffTeachersKidsArePagedAtAConstantCost() {
        assertPaging("the kids a staff teacher may see", this.kidIdsByName,
                     page -> this.kidRepository.findByClassroomTaughtOrTutoredBy(this.staffTeacherId,
                                                                                 PageRequest.of(page, PAGE_SIZE)),
                     Kid::getId);
    }

    @Test
    void aClassroomsTeachingStaffIsPagedWithoutTheManyToManyLeakingIntoTheTotal() {
        assertPaging("a classroom's teaching staff", this.classroomStaffIds,
                     page -> this.teacherRepository.findByClassroomId(this.classroomId,
                                                                      PageRequest.of(page, PAGE_SIZE)),
                     Teacher::getId);
    }

    @Test
    void aSchoolsTeachersArePagedAtAConstantCostDespiteTheFetchGraph() {
        assertPaging("a school's teachers", this.schoolTeacherIds,
                     page -> this.teacherRepository.findBySchoolId(this.schoolId, PageRequest.of(page, PAGE_SIZE)),
                     Teacher::getId);
    }

    @Test
    void aSchoolsClassroomsArePagedAtAConstantCost() {
        final List<UUID> classrooms =
                this.classroomRepository.findBySchoolIdOrderByCourseAscGroupAsc(this.schoolId, PageRequest.of(0, 10))
                                        .map(Classroom::getId).getContent();
        this.entityManager.clear();
        assertPaging("a school's classrooms", classrooms,
                     page -> this.classroomRepository.findBySchoolIdOrderByCourseAscGroupAsc(this.schoolId,
                                                                              PageRequest.of(page, PAGE_SIZE)),
                     Classroom::getId);
    }

    @Test
    void parentsArePagedAtAConstantCostDespiteTheFetchGraph() {
        assertPaging("parents", this.parentIds,
                     page -> this.parentRepository.findAll(PageRequest.of(page, PAGE_SIZE)), Parent::getId);
    }

    @Test
    void aParentsKidsComeBackInNameOrderAcrossPageBoundaries() {
        final List<String> names = new ArrayList<>();
        for(int page = 0; page * PAGE_SIZE < KIDS; page++) {
            this.kidRepository.findByParentIdOrderByNameAsc(this.parentId, PageRequest.of(page, PAGE_SIZE))
                              .forEach(kid -> names.add(kid.getName()));
        }
        assertEquals(names.stream().sorted().toList(), names, "ordering must hold across page boundaries, not only "
                                                              + "within a page");
    }

    @Test
    void anOutOfRangePageIsEmptyAndStillReportsTheRealTotal() {
        final Page<Kid> page = this.kidRepository.findByParentIdOrderByNameAsc(this.parentId,
                                                                               PageRequest.of(99, PAGE_SIZE));
        assertTrue(page.getContent().isEmpty(), "a page past the end must be empty");
        assertEquals(KIDS, page.getTotalElements(), "a page past the end must still report the real total");
    }

    /**
     * Walks every page of a query and checks both halves of the paging contract at once: the pages partition the
     * expected ids exactly once, report a stable total, and each costs one query plus its count - never one query per
     * row.
     *
     * @param what     what is being paged, for assertion messages
     * @param expected the ids the pages must add up to, in the order the query promises
     * @param fetch    fetches the given page
     * @param idOf     reads an element's id
     * @param <T>      the element type
     */
    private <T> void assertPaging(
            final String what,
            final List<UUID> expected,
            final IntFunction<Page<T>> fetch,
            final Function<T, UUID> idOf
    ) {
        final int        pages = (expected.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        final List<UUID> seen  = new ArrayList<>();

        for(int number = 0; number < pages; number++) {
            this.entityManager.clear();
            final Statistics statistics = this.clearedStatistics();

            final Page<T>    page    = fetch.apply(number);
            final List<UUID> content = page.getContent().stream().map(idOf).toList();

            final boolean full = content.size() == PAGE_SIZE;
            assertEquals(full ? QUERY_PLUS_COUNT : QUERY_ONLY, statistics.getPrepareStatementCount(),
                         "%s: page %d must cost one query plus its count, never one query per row"
                                 .formatted(what, number));
            assertEquals(expected.size(), page.getTotalElements(),
                         "%s: page %d must report the number of entities, not of joined rows".formatted(what, number));
            assertEquals(pages, page.getTotalPages(), "%s: page %d reports the wrong page count".formatted(what,
                                                                                                           number));
            assertEquals(content.size(), content.stream().distinct().count(),
                         "%s: page %d contains the same entity more than once".formatted(what, number));
            seen.addAll(content);
        }

        assertEquals(expected.size(), seen.size(),
                     "%s: the pages must add up to the whole result set, with nothing skipped or repeated"
                             .formatted(what));
        assertEquals(expected, seen, "%s: the pages must cover the result set in the promised order".formatted(what));
    }

    private Statistics clearedStatistics() {
        final Statistics statistics = this.em.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();
        return statistics;
    }

    private static Person aPerson(final String slug) {
        return Person.builder().id(UUID.randomUUID()).name(slug).surname(slug).mail(slug + "@example.com").build();
    }

}
