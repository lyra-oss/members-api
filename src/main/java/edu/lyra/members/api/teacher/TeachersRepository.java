package edu.lyra.members.api.teacher;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface TeachersRepository
        extends CrudRepository<Teacher, UUID>, ListPagingAndSortingRepository<Teacher, UUID> {}
