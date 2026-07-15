package edu.lyra.members.api.repositories.jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface KidsRepository
        extends CrudRepository<Kid, UUID>, ListPagingAndSortingRepository<Kid, UUID> {

    Page<Kid> findByParentIdOrderByNameAsc(final UUID parentId, final Pageable pageable);

    @Query(
            value = "select distinct k from Kid k join k.classroom c left join c.teachers t " +
                    "where c.tutor.id = :teacherId or t.id = :teacherId order by k.name",
            countQuery = "select count(distinct k) from Kid k join k.classroom c left join c.teachers t " +
                         "where c.tutor.id = :teacherId or t.id = :teacherId"
    )
    Page<Kid> findByClassroomTaughtOrTutoredBy(final @Param("teacherId") UUID teacherId, final Pageable pageable);

}
