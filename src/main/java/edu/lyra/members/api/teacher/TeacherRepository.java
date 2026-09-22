package edu.lyra.members.api.teacher;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for {@link Teacher}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Repository
@Transactional
public interface TeacherRepository
        extends CrudRepository<Teacher, UUID>, ListPagingAndSortingRepository<Teacher, UUID> {

    /**
     * Finds a page of teachers, fetching each one's {@code person} in the same query.
     *
     * <p>{@code Teacher} delegates its identity fields to {@code Person}, so every teacher rendered by the API reads
     * them. Without the fetch graph the lazy association turns one page into one query per row.
     *
     *
     * <p>Ordered by name, then surname, then id. The id is the primary key, so the order is total: without one,
     * nothing obliges the database to produce the same order for each page's query, and {@code LIMIT}/{@code OFFSET}
     * can then return a row on two pages and skip it on a third.
     *
     * @param pageable the requested page
     *
     * @return the matching page of teachers
     */
    @Override
    @EntityGraph(attributePaths = "person")
    @Query(
            value = "select t from Teacher t order by t.person.name, t.person.surname, t.id",
            countQuery = "select count(t) from Teacher t"
    )
    Page<Teacher> findAll(final Pageable pageable);

    /**
     * Finds a page of the teachers at the given school.
     *
     * <p>Ordered by name, then surname, then id. The id is the primary key, so the order is total: without one,
     * nothing obliges the database to produce the same order for each page's query, and {@code LIMIT}/{@code OFFSET}
     * can then return a row on two pages and skip it on a third.
     *
     * @param schoolId the school's id
     * @param pageable the requested page
     *
     * @return the matching page of teachers
     */
    @EntityGraph(attributePaths = "person")
    @Query(
            value = "select t from Teacher t where t.school.id = :schoolId " +
                    "order by t.person.name, t.person.surname, t.id",
            countQuery = "select count(t) from Teacher t where t.school.id = :schoolId"
    )
    Page<Teacher> findBySchoolId(final @Param("schoolId") UUID schoolId, final Pageable pageable);

    /**
     * Counts the teachers at the given school.
     *
     * <p>Callers that only need to know whether a school still has teachers must use this instead of walking an
     * association, so the database answers with a count rather than the application loading every row.
     *
     * @param schoolId the school's id
     *
     * @return the number of teachers at that school
     */
    long countBySchoolId(final UUID schoolId);

    /**
     * Finds a page of the teaching staff (not including the tutor unless also a member of the teaching staff) of the
     * given classroom.
     *
     *
     * <p>Ordered by name, then surname, then id. The id is the primary key, so the order is total: without one,
     * nothing obliges the database to produce the same order for each page's query, and {@code LIMIT}/{@code OFFSET}
     * can then return a row on two pages and skip it on a third.
     *
     * <p>No {@code distinct} is needed: {@code c.id} is the primary key, so exactly one classroom matches, and the
     * join table's own primary key admits each teacher of it once. Dropping it also keeps the ordering legal -
     * {@code select distinct} may only be ordered by expressions in its select list, and the order below reads
     * through to the teacher's person.
     *
     * @param classroomId the classroom's id
     * @param pageable    the requested page
     *
     * @return the matching page of teachers
     */
    @Query(
            value = "select t from Classroom c join c.teachers t where c.id = :classroomId " +
                    "order by t.person.name, t.person.surname, t.id",
            countQuery = "select count(t) from Classroom c join c.teachers t where c.id = :classroomId"
    )
    @EntityGraph(attributePaths = "person")
    Page<Teacher> findByClassroomId(final @Param("classroomId") UUID classroomId, final Pageable pageable);

}
