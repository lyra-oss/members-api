package edu.lyra.members.api.school;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface SchoolRepository
        extends CrudRepository<School, UUID>, ListPagingAndSortingRepository<School, UUID> {}
