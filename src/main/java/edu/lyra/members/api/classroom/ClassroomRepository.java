package edu.lyra.members.api.classroom;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ClassroomRepository
        extends CrudRepository<Classroom, UUID>, ListPagingAndSortingRepository<Classroom, UUID> {

    @Query(
            "select case when count(c) > 0 then true else false end from Classroom c left join c.teachers t " +
            "where c.tutor.id = :teacherId or t.id = :teacherId"
    )
    boolean existsByTutorIdOrTeachersId(final @Param("teacherId") UUID teacherId);

}
