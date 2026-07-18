package edu.lyra.members.api.teacher;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
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
        extends CrudRepository<Teacher, UUID>, ListPagingAndSortingRepository<Teacher, UUID> {}
