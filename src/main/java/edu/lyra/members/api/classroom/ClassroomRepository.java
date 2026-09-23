package edu.lyra.members.api.classroom;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for {@link Classroom}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Repository
@Transactional
public interface ClassroomRepository
        extends CrudRepository<Classroom, UUID>, ListPagingAndSortingRepository<Classroom, UUID> {

    /**
     * Finds a page of the classrooms at the given school, ordered by course and then group.
     *
     * <p>That order is the one the school's unique constraint is already stored in
     * ({@code SCHOOL_ID, COURSE, GROUP_NAME}), so the same index that narrows to the school also returns the rows
     * sorted and the database does no sort of its own. It also makes paging deterministic: without an order,
     * {@code LIMIT}/{@code OFFSET} may repeat a row on one page and skip it on the next.
     *
     * @param schoolId the school's id
     * @param pageable the requested page
     *
     * @return the matching page of classrooms
     */
    Page<Classroom> findBySchoolIdOrderByCourseAscGroupAsc(final UUID schoolId, final Pageable pageable);

    /**
     * Counts the classrooms at the given school.
     *
     * <p>Callers that only need to know whether a school still has classrooms must use this instead of walking an
     * association, so the database answers with a count rather than the application loading every row.
     *
     * @param schoolId the school's id
     *
     * @return the number of classrooms at that school
     */
    long countBySchoolId(final UUID schoolId);

    /**
     * Checks whether the given teacher tutors or teaches at least one classroom.
     *
     * @param teacherId the teacher's id
     *
     * @return {@code true} if the teacher is the tutor of, or a member of the teaching staff of, at least one
     * classroom; {@code false} otherwise
     */
    @Query(
            "select case when count(c) > 0 then true else false end from Classroom c left join c.teachers t " +
            "where c.tutor.id = :teacherId or t.id = :teacherId"
    )
    boolean existsByTutorIdOrTeachersId(final @Param("teacherId") UUID teacherId);

}
