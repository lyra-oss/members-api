package edu.lyra.members.api.kid;

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
 * Spring Data repository for {@link Kid}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Repository
@Transactional
public interface KidRepository
        extends CrudRepository<Kid, UUID>, ListPagingAndSortingRepository<Kid, UUID> {

    /**
     * Finds a page of the given parent's kids, ordered by name.
     *
     * @param parentId the parent's id
     * @param pageable the requested page
     *
     * @return the matching page of kids
     */
    Page<Kid> findByParentIdOrderByNameAsc(final UUID parentId, final Pageable pageable);

    /**
     * Finds a page of the kids in classrooms the given teacher tutors or teaches, ordered by name.
     *
     * @param teacherId the teacher's id
     * @param pageable the requested page
     * @return the matching page of kids
     */
    @Query(
            value = "select distinct k from Kid k join k.classroom c left join c.teachers t " +
                    "where c.tutor.id = :teacherId or t.id = :teacherId order by k.name",
            countQuery = "select count(distinct k) from Kid k join k.classroom c left join c.teachers t " +
                         "where c.tutor.id = :teacherId or t.id = :teacherId"
    )
    Page<Kid> findByClassroomTaughtOrTutoredBy(final @Param("teacherId") UUID teacherId, final Pageable pageable);

}
