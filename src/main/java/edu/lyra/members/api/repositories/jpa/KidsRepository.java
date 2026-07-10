package edu.lyra.members.api.repositories.jpa;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface KidsRepository
        extends CrudRepository<Kid, UUID>, ListPagingAndSortingRepository<Kid, UUID> {}
