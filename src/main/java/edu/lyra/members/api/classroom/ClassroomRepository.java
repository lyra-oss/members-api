package edu.lyra.members.api.classroom;

import java.util.UUID;

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
