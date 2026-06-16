package edu.lyra.members.api.repositories.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ParentsRepository
        extends CrudRepository<Parent, UUID>, ListPagingAndSortingRepository<Parent, UUID> {

    Optional<Parent> findByMail(final String mail);

}
