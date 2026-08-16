package edu.lyra.members.api.teacher;

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
 * Spring Data repository for {@link Teacher}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Repository
@Transactional
public interface TeacherRepository
        extends CrudRepository<Teacher, UUID>, ListPagingAndSortingRepository<Teacher, UUID> {

    /**
     * Finds a page of the teachers at the given school.
     *
     * @param schoolId the school's id
     * @param pageable the requested page
     *
     * @return the matching page of teachers
     */
    Page<Teacher> findBySchoolId(final UUID schoolId, final Pageable pageable);

    /**
     * Finds a page of the teaching staff (not including the tutor unless also a member of the teaching staff) of the
     * given classroom.
     *
     * @param classroomId the classroom's id
     * @param pageable    the requested page
     *
     * @return the matching page of teachers
     */
    @Query(
            value = "select distinct t from Classroom c join c.teachers t where c.id = :classroomId",
            countQuery = "select count(distinct t) from Classroom c join c.teachers t where c.id = :classroomId"
    )
    Page<Teacher> findByClassroomId(final @Param("classroomId") UUID classroomId, final Pageable pageable);

}
