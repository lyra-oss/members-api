package edu.lyra.members.api.parent;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
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
        extends CrudRepository<Parent, UUID>, ListPagingAndSortingRepository<Parent, UUID> {

    /**
     * Finds a page of parents, fetching each one's {@code person} in the same query.
     *
     * <p>{@code Parent} delegates its identity fields to {@code Person}, so every parent rendered by the API reads
     * them. Without the fetch graph the lazy association turns one page into one query per row.
     *
     * <p>Ordered by name, then surname, then id. The id is the primary key, so the order is total: without one,
     * nothing obliges the database to produce the same order for each page's query, and {@code LIMIT}/{@code OFFSET}
     * can then return a row on two pages and skip it on a third.
     *
     * @param pageable the requested page
     *
     * @return the matching page of parents
     */
    @Override
    @EntityGraph(attributePaths = "person")
    @Query(
            value = "select p from Parent p order by p.person.name, p.person.surname, p.id",
            countQuery = "select count(p) from Parent p"
    )
    Page<Parent> findAll(final Pageable pageable);

}
