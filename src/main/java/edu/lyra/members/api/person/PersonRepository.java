package edu.lyra.members.api.person;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface PersonRepository
        extends CrudRepository<Person, UUID>, ListPagingAndSortingRepository<Person, UUID> {

    Optional<Person> findByMail(final String mail);

}
