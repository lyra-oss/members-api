package edu.lyra.members.api.classroom;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ClassroomsRepository
        extends CrudRepository<Classroom, UUID>, ListPagingAndSortingRepository<Classroom, UUID> {}
