package edu.lyra.members.api.person;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for {@link Person}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Repository
@Transactional
public interface PersonRepository
        extends CrudRepository<Person, UUID>, ListPagingAndSortingRepository<Person, UUID> {

    /**
     * Finds the person with the given email address, if any.
     *
     * @param mail the email address to search for
     *
     * @return the matching person, or {@link Optional#empty()} if none exists
     */
    Optional<Person> findByMail(final String mail);

}
