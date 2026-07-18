package edu.lyra.members.api.parent;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for {@link Parent}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Repository
@Transactional
public interface ParentRepository
        extends CrudRepository<Parent, UUID>, ListPagingAndSortingRepository<Parent, UUID> {}
